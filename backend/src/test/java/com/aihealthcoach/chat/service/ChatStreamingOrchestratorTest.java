package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamErrorEvent;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamToolResultEvent;
import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.meal.service.AiMealProposalService;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.memory.service.UserMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

class ChatStreamingOrchestratorTest {

    private static final Long USER_ID = 1L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-08T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final RecordingChatService chatService = new RecordingChatService();
    private final ControllableContextBuilder contextBuilder = new ControllableContextBuilder();
    private final PromptBuilder promptBuilder = new PromptBuilderImpl(new AiPromptFactory());
    private final FakeAssistantStreamingLlm assistantLlm = new FakeAssistantStreamingLlm();
    private final FakeToolLlm toolLlm = new FakeToolLlm();
    private final RecordingUserMemoryService userMemoryService = new RecordingUserMemoryService();
    private final RecordingSink sink = new RecordingSink();

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void toolStartsWhileAssistantWaitsForRequiredContext() throws Exception {
        contextBuilder.block();
        toolLlm.respondWith(noToolJson());
        assistantLlm.emit("안녕");
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        Thread streamThread = new Thread(() -> orchestrator.stream(USER_ID, request("점심 먹었어"), sink));
        streamThread.start();

        assertThat(toolLlm.started.await(1, TimeUnit.SECONDS)).isTrue();
        assertThat(assistantLlm.started.get()).isFalse();
        assertThat(toolLlm.lastRequest.dynamicContextPrompt()).doesNotContain("user_profile");
        assertThat(toolLlm.lastRequest.userMessage()).isEqualTo("점심 먹었어");

        contextBuilder.release(emptyContext());
        streamThread.join(2_000);

        assertThat(assistantLlm.started.get()).isTrue();
        assertThat(sink.eventNames()).containsSubsequence("delta", "assistant_done", "tool_result", "done");
    }

    @Test
    void userMessageSaveFailurePreventsLlmCallsAndDelta() {
        chatService.failUserSave = true;
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("안녕"), sink);

        assertThat(assistantLlm.started.get()).isFalse();
        assertThat(toolLlm.started.getCount()).isEqualTo(1);
        assertThat(sink.eventNames()).containsExactly("error");
        assertThat(((ChatStreamErrorEvent) sink.events.getFirst().data()).code())
                .isEqualTo("USER_MESSAGE_SAVE_FAILED");
    }

    @Test
    void assistantSaveFailureDoesNotSendAssistantDone() {
        chatService.failAssistantSave = true;
        toolLlm.respondWith(noToolJson());
        assistantLlm.emit("저장 실패 전까지 보인 답변");
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("안녕"), sink);

        assertThat(sink.eventNames()).contains("delta", "error");
        assertThat(sink.eventNames()).doesNotContain("assistant_done", "tool_result", "done");
        assertThat(((ChatStreamErrorEvent) sink.lastEvent().data()).code())
                .isEqualTo("ASSISTANT_MESSAGE_SAVE_FAILED");
    }

    @Test
    void assistantStreamFailureSendsErrorEventAndSkipsAssistantSave() {
        toolLlm.respondWith(noToolJson());
        assistantLlm.emit("일부 답변");
        assistantLlm.failWith(new IllegalStateException("assistant provider failed"));
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("답변 중 실패해줘"), sink);

        assertThat(sink.eventNames()).containsExactly("delta", "error");
        assertThat(((ChatStreamErrorEvent) sink.lastEvent().data()).code())
                .isEqualTo("ASSISTANT_STREAM_FAILED");
        assertThat(chatService.savedAssistantContent).isNull();
        assertThat(chatService.saveCount.get()).isEqualTo(1);
    }

    @Test
    void assistantStreamFailureBeforeFirstDeltaSendsOnlyErrorEvent() {
        toolLlm.respondWith(noToolJson());
        assistantLlm.failWith(new IllegalStateException("assistant provider failed"));
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("시작 전 실패해줘"), sink);

        assertThat(sink.eventNames()).containsExactly("error");
        assertThat(((ChatStreamErrorEvent) sink.lastEvent().data()).code())
                .isEqualTo("ASSISTANT_STREAM_FAILED");
        assertThat(chatService.savedAssistantContent).isNull();
        assertThat(chatService.saveCount.get()).isEqualTo(1);
    }

    @Test
    void clientDisconnectDuringDeltaSkipsAssistantSaveAndSuppressesErrorDispatch() {
        toolLlm.respondWith(noToolJson());
        assistantLlm.emit("끊긴 뒤 저장되면 안 되는 답변");
        sink.failOnSend("delta", "error");
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("중간에 연결 끊김"), sink);

        assertThat(sink.eventNames()).isEmpty();
        assertThat(chatService.savedAssistantContent).isNull();
        assertThat(chatService.saveCount.get()).isEqualTo(1);
        assertThat(sink.completeCount).isZero();
        assertThat(sink.completeWithErrorCount).isEqualTo(1);
    }

    @Test
    void toolFailureDoesNotBreakAssistantStream() {
        toolLlm.failWith(new IllegalStateException("provider failed"));
        assistantLlm.emit("답변은 정상");
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("운동 기록해줘"), sink);

        assertThat(sink.eventNames()).containsExactly("delta", "assistant_done", "tool_result", "done");
        ChatStreamToolResultEvent toolResult = (ChatStreamToolResultEvent) sink.event("tool_result").data();
        assertThat(toolResult.status()).isEqualTo("FAILED");
        assertThat(toolResult.reason()).isEqualTo("GENERATION_FAILED");
        assertThat(chatService.savedAssistantContent).isEqualTo("답변은 정상");
    }

    @Test
    void invalidToolJsonDoesNotLeakRawJsonToStream() {
        toolLlm.respondWith("not-json");
        assistantLlm.emit("답변");
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("점심 먹었어"), sink);

        assertThat(sink.eventNames()).containsExactly("delta", "assistant_done", "tool_result", "done");
        assertThat(sink.events).noneMatch(event -> "not-json".equals(event.data()));
        ChatStreamToolResultEvent toolResult = (ChatStreamToolResultEvent) sink.event("tool_result").data();
        assertThat(toolResult.status()).isEqualTo("FAILED");
        assertThat(toolResult.reason()).isEqualTo("PARSE_FAILED");
    }

    @Test
    void memorySaveFailureIsReportedInToolResultOnly() {
        toolLlm.respondWith("""
                {
                  "assistantMessage": "확인했어요.",
                  "memorySaveCommand": {
                    "memorySaveIntent": true,
                    "content": "유제품은 피하고 싶어"
                  }
                }
                """);
        assistantLlm.emit("기억 요청을 확인했어요.");
        userMemoryService.failCreate = true;
        ChatStreamingOrchestrator orchestrator = newOrchestrator();

        orchestrator.stream(USER_ID, request("유제품 피하는 걸 기억해줘"), sink);

        assertThat(sink.eventNames()).containsExactly("delta", "assistant_done", "tool_result", "done");
        ChatStreamToolResultEvent toolResult = (ChatStreamToolResultEvent) sink.event("tool_result").data();
        assertThat(toolResult.status()).isEqualTo("SUCCESS");
        assertThat(toolResult.memorySave().status()).isEqualTo("FAILED");
        assertThat(toolResult.memorySave().reason()).isEqualTo("SAVE_FAILED");
        assertThat(chatService.savedAssistantContent).isEqualTo("기억 요청을 확인했어요.");
    }

    private ChatStreamingOrchestrator newOrchestrator() {
        AiMealProposalService mealProposalService = extracted -> null;
        AiExerciseProposalService exerciseProposalService = extracted -> null;
        return new ChatStreamingOrchestrator(
                chatService,
                contextBuilder,
                promptBuilder,
                assistantLlm,
                toolLlm,
                mealProposalService,
                exerciseProposalService,
                userMemoryService,
                new ObjectMapper().findAndRegisterModules(),
                CLOCK,
                executor
        );
    }

    private ChatMessageRequest request(String content) {
        return new ChatMessageRequest(content);
    }

    private UserChatContext emptyContext() {
        return new UserChatContext(null, null, null, List.of(), List.of(), List.of(), List.of());
    }

    private String noToolJson() {
        return """
                {
                  "assistantMessage": "확인했어요.",
                  "mealExtraction": {"mealIntent": false},
                  "exerciseExtraction": {"exerciseIntent": false},
                  "weightExtraction": {"weightIntent": false},
                  "memorySaveCommand": {"memorySaveIntent": false}
                }
                """;
    }

    private static class FakeAssistantStreamingLlm implements AssistantStreamingLlmService {

        private final AtomicBoolean started = new AtomicBoolean(false);
        private final CountDownLatch startedLatch = new CountDownLatch(1);
        private final List<String> chunks = new ArrayList<>();
        private RuntimeException exception;

        void emit(String... chunks) {
            this.chunks.clear();
            this.chunks.addAll(List.of(chunks));
        }

        void failWith(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public void streamAssistantMessage(LlmRequest request, Consumer<String> onDelta) {
            started.set(true);
            startedLatch.countDown();
            chunks.forEach(onDelta);
            if (exception != null) {
                throw exception;
            }
        }
    }

    private static class FakeToolLlm implements LlmService {

        private final CountDownLatch started = new CountDownLatch(1);
        private String response = """
                {"assistantMessage":"확인했어요."}
                """;
        private RuntimeException exception;
        private LlmRequest lastRequest;

        void respondWith(String response) {
            this.response = response;
        }

        void failWith(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public LlmResponse generate(LlmRequest request) {
            this.lastRequest = request;
            started.countDown();
            if (exception != null) {
                throw exception;
            }
            return new LlmResponse(response);
        }
    }

    private static class ControllableContextBuilder implements ContextBuilder {

        private final AtomicBoolean shouldBlock = new AtomicBoolean(false);
        private CountDownLatch releaseLatch = new CountDownLatch(0);
        private UserChatContext context;

        void block() {
            shouldBlock.set(true);
            releaseLatch = new CountDownLatch(1);
        }

        void release(UserChatContext context) {
            this.context = context;
            releaseLatch.countDown();
        }

        @Override
        public UserChatContext build(Long userId, LocalDate contextDate) {
            if (shouldBlock.get()) {
                try {
                    releaseLatch.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }
            }
            return context;
        }
    }

    private static class RecordingChatService implements ChatService {

        private boolean failUserSave;
        private boolean failAssistantSave;
        private String savedAssistantContent;
        private final AtomicInteger saveCount = new AtomicInteger();

        @Override
        public List<ChatMessageResponse> findMessagesByUserId(Long userId) {
            return List.of();
        }

        @Override
        public List<ChatMessageResponse> findRecentMessages(Long userId, int limit) {
            return List.of();
        }

        @Override
        public ChatMessageResponse insert(Long userId, ChatMessageRequest message) {
            if (failUserSave) {
                throw new IllegalStateException("user save failed");
            }
            saveCount.incrementAndGet();
            return message("USER", message.content());
        }

        @Override
        public ChatMessageResponse insert(ChatMessage message) {
            if (failAssistantSave) {
                throw new IllegalStateException("assistant save failed");
            }
            saveCount.incrementAndGet();
            savedAssistantContent = message.getContent();
            return message("ASSISTANT", message.getContent());
        }

        private ChatMessageResponse message(String role, String content) {
            return new ChatMessageResponse(role, content, LocalDateTime.of(2026, 6, 8, 12, 0));
        }
    }

    private static class RecordingUserMemoryService implements UserMemoryService {

        private boolean failCreate;

        @Override
        public UserMemoryResponse createMemory(Long userId, UserMemoryCreateRequest request) {
            if (failCreate) {
                throw new IllegalStateException("memory save failed");
            }
            return null;
        }

        @Override
        public void deactivateMemory(Long userId, Long memoryId) {
        }

        @Override
        public List<UserMemoryResponse> findMemoriesByUserId(Long userId) {
            return List.of();
        }

        @Override
        public List<UserMemoryResponse> findActiveMemories(Long userId, int limit) {
            return List.of();
        }
    }

    private static class RecordingSink implements ChatStreamEventSink {

        private final List<RecordedEvent> events = new ArrayList<>();
        private final List<String> failedEventNames = new ArrayList<>();
        private int completeCount;
        private int completeWithErrorCount;

        void failOnSend(String... eventNames) {
            failedEventNames.clear();
            failedEventNames.addAll(List.of(eventNames));
        }

        @Override
        public synchronized void send(String eventName, Object data) {
            if (failedEventNames.contains(eventName)) {
                throw new IllegalStateException("simulated client disconnect");
            }
            events.add(new RecordedEvent(eventName, data));
        }

        @Override
        public void complete() {
            completeCount++;
        }

        @Override
        public void completeWithError(Throwable error) {
            completeWithErrorCount++;
        }

        List<String> eventNames() {
            return events.stream().map(RecordedEvent::name).toList();
        }

        RecordedEvent event(String name) {
            return events.stream()
                    .filter(event -> event.name().equals(name))
                    .findFirst()
                    .orElseThrow();
        }

        RecordedEvent lastEvent() {
            return events.getLast();
        }
    }

    private record RecordedEvent(String name, Object data) {
    }
}
