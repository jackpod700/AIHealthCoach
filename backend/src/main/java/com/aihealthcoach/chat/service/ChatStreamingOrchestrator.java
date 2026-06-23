package com.aihealthcoach.chat.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamAssistantDoneEvent;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamDeltaEvent;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamDoneEvent;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamErrorEvent;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamMemorySaveResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatStreamToolResultEvent;
import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.meal.service.AiMealProposalService;
import com.aihealthcoach.memory.dto.UserMemoryDto.MemorySaveCommand;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.service.UserMemoryService;
import com.aihealthcoach.weight.dto.AiWeightDto.ExtractedWeightResult;
import com.aihealthcoach.weight.dto.AiWeightDto.WeightProposalResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatStreamingOrchestrator {

    private static final int MAX_MEMORY_CONTENT_LENGTH = 500;
    private static final String EVENT_DELTA = "delta";
    private static final String EVENT_ASSISTANT_DONE = "assistant_done";
    private static final String EVENT_TOOL_RESULT = "tool_result";
    private static final String EVENT_ERROR = "error";
    private static final String EVENT_DONE = "done";

    private final ChatService chatService;
    private final ContextBuilder contextBuilder;
    private final PromptBuilder promptBuilder;
    private final AssistantStreamingLlmService assistantStreamingLlmService;
    private final LlmService llmService;
    private final AiMealProposalService aiMealProposalService;
    private final AiExerciseProposalService aiExerciseProposalService;
    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Executor executor;

    public ChatStreamingOrchestrator(
            ChatService chatService,
            ContextBuilder contextBuilder,
            PromptBuilder promptBuilder,
            AssistantStreamingLlmService assistantStreamingLlmService,
            LlmService llmService,
            AiMealProposalService aiMealProposalService,
            AiExerciseProposalService aiExerciseProposalService,
            UserMemoryService userMemoryService,
            ObjectMapper objectMapper,
            Clock clock,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.chatService = chatService;
        this.contextBuilder = contextBuilder;
        this.promptBuilder = promptBuilder;
        this.assistantStreamingLlmService = assistantStreamingLlmService;
        this.llmService = llmService;
        this.aiMealProposalService = aiMealProposalService;
        this.aiExerciseProposalService = aiExerciseProposalService;
        this.userMemoryService = userMemoryService;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.executor = executor;
    }

    void stream(Long userId, ChatMessageRequest request, ChatStreamEventSink sink) {
        ChatStreamTiming timing = ChatStreamTiming.start();
        String userMessage = request == null ? null : request.content();
        try {
            chatService.insert(userId, request);
            timing.markUserSaved();
        } catch (RuntimeException exception) {
            log.warn("Failed to save chat stream user message. user_id={}", userId, exception);
            sendErrorAndComplete(sink, "USER_MESSAGE_SAVE_FAILED", "메시지를 저장하지 못했어요.");
            return;
        }

        LocalDate contextDate = LocalDate.now(clock);
        CompletableFuture<UserChatContext> contextFuture = CompletableFuture.supplyAsync(
                () -> buildContext(userId, contextDate),
                executor
        );
        CompletableFuture<ChatStreamToolResultEvent> toolFuture = CompletableFuture.supplyAsync(
                () -> {
                    ChatStreamToolResultEvent result = buildToolResult(userId, userMessage, contextDate);
                    timing.markToolCompleted();
                    return result;
                },
                executor
        );

        StringBuilder assistantContent = new StringBuilder();
        AtomicBoolean firstDeltaSeen = new AtomicBoolean(false);
        try {
            UserChatContext context = waitForContext(userId, contextFuture, timing);
            LlmRequest assistantRequest = promptBuilder.buildAssistantStream(contextDate, userMessage, context);
            timing.markAssistantPromptBuilt();
            assistantStreamingLlmService.streamAssistantMessage(assistantRequest, delta -> {
                if (firstDeltaSeen.compareAndSet(false, true)) {
                    timing.markFirstDelta();
                }
                assistantContent.append(delta);
                sink.send(EVENT_DELTA, new ChatStreamDeltaEvent(delta));
            });
            timing.markAssistantStreamCompleted();
        } catch (RuntimeException exception) {
            log.warn("Failed to stream assistant chat message. user_id={}", userId, exception);
            toolFuture.cancel(true);
            sendErrorAndComplete(sink, "ASSISTANT_STREAM_FAILED", "답변을 생성하지 못했어요.");
            return;
        }

        ChatMessageResponse assistantMessage;
        try {
            assistantMessage = chatService.insert(ChatMessage.builder()
                    .userId(userId)
                    .role("ASSISTANT")
                    .content(assistantContent.toString())
                    .build());
            timing.markAssistantSaved();
        } catch (RuntimeException exception) {
            log.warn("Failed to save streamed assistant message. user_id={}", userId, exception);
            toolFuture.cancel(true);
            sendErrorAndComplete(sink, "ASSISTANT_MESSAGE_SAVE_FAILED", "답변을 저장하지 못했어요.");
            return;
        }

        try {
            sink.send(EVENT_ASSISTANT_DONE, new ChatStreamAssistantDoneEvent(assistantMessage));
            sink.send(EVENT_TOOL_RESULT, toolFuture.join());
            timing.markToolJoined();
            sink.send(EVENT_DONE, ChatStreamDoneEvent.done());
            sink.complete();
            log.info(
                    "chat_stream_timing user_id={} user_save_ms={} context_wait_ms={} assistant_prompt_ms={} first_delta_ms={} assistant_stream_ms={} assistant_save_ms={} tool_total_ms={} tool_join_wait_ms={} total_ms={}",
                    userId,
                    timing.userSaveMs(),
                    timing.contextWaitMs(),
                    timing.assistantPromptMs(),
                    timing.firstDeltaMs(),
                    timing.assistantStreamMs(),
                    timing.assistantSaveMs(),
                    timing.toolTotalMs(),
                    timing.toolJoinWaitMs(),
                    timing.totalMs()
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to complete chat stream. user_id={}", userId, exception);
            sink.completeWithError(exception);
        }
    }

    private UserChatContext waitForContext(
            Long userId,
            CompletableFuture<UserChatContext> contextFuture,
            ChatStreamTiming timing
    ) {
        timing.markContextWaitStarted();
        try {
            UserChatContext context = contextFuture.get();
            timing.markContextWaitCompleted();
            return context;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for chat stream context", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("Failed to wait for chat stream context", exception);
        }
    }

    private UserChatContext buildContext(Long userId, LocalDate contextDate) {
        return contextBuilder.build(userId, contextDate);
    }

    private ChatStreamToolResultEvent buildToolResult(Long userId, String userMessage, LocalDate contextDate) {
        LlmResponse response;
        try {
            LlmRequest request = promptBuilder.buildToolJson(contextDate, userMessage);
            response = llmService.generate(request);
        } catch (RuntimeException exception) {
            log.warn("Failed to generate tool JSON for chat stream. user_id={}", userId, exception);
            return ChatStreamToolResultEvent.failed("GENERATION_FAILED");
        }

        AiChatResult result;
        try {
            result = parseToolResult(response.content());
        } catch (RuntimeException exception) {
            log.warn("Failed to parse tool JSON for chat stream. user_id={}", userId, exception);
            return ChatStreamToolResultEvent.failed("PARSE_FAILED");
        }

        try {
            ChatStreamMemorySaveResult memorySave = saveMemoryIfRequested(userId, result.memorySaveCommand());
            return ChatStreamToolResultEvent.success(
                    aiMealProposalService.createProposal(result.mealExtraction()),
                    aiExerciseProposalService.createProposal(result.exerciseExtraction()),
                    WeightProposalResponse.fromExtraction(result.weightExtraction()),
                    memorySave
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to convert chat stream tool result. user_id={}", userId, exception);
            return ChatStreamToolResultEvent.failed("PROPOSAL_FAILED");
        }
    }

    private AiChatResult parseToolResult(String content) {
        try {
            AiChatResult result = objectMapper.readValue(content, AiChatResult.class);
            return normalizeToolResult(result);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse tool JSON", exception);
        }
    }

    private AiChatResult normalizeToolResult(AiChatResult result) {
        if (result == null) {
            return emptyToolResult();
        }
        return new AiChatResult(
                result.assistantMessage(),
                result.mealExtraction() == null ? ExtractedMealResult.noMeal() : result.mealExtraction(),
                result.exerciseExtraction() == null ? ExtractedExerciseResult.noExercise() : result.exerciseExtraction(),
                result.weightExtraction() == null ? ExtractedWeightResult.noWeight() : result.weightExtraction(),
                normalizeMemorySaveCommand(result.memorySaveCommand())
        );
    }

    private AiChatResult emptyToolResult() {
        return new AiChatResult(
                "",
                ExtractedMealResult.noMeal(),
                ExtractedExerciseResult.noExercise(),
                ExtractedWeightResult.noWeight(),
                MemorySaveCommand.noCommand()
        );
    }

    private MemorySaveCommand normalizeMemorySaveCommand(MemorySaveCommand command) {
        if (command == null || !command.memorySaveIntent() || command.content() == null || command.content().isBlank()) {
            return MemorySaveCommand.noCommand();
        }
        return new MemorySaveCommand(true, command.content().trim());
    }

    private ChatStreamMemorySaveResult saveMemoryIfRequested(Long userId, MemorySaveCommand command) {
        if (command == null || !command.memorySaveIntent()) {
            return ChatStreamMemorySaveResult.none();
        }
        if (command.content() == null || command.content().isBlank()
                || command.content().length() > MAX_MEMORY_CONTENT_LENGTH) {
            return ChatStreamMemorySaveResult.failed("INVALID_CONTENT");
        }
        try {
            userMemoryService.createMemory(userId, new UserMemoryCreateRequest(command.content()));
            return ChatStreamMemorySaveResult.saved();
        } catch (RuntimeException exception) {
            log.warn("Failed to save memory from chat stream tool result. user_id={}", userId, exception);
            return ChatStreamMemorySaveResult.failed("SAVE_FAILED");
        }
    }

    private void sendErrorAndComplete(ChatStreamEventSink sink, String code, String message) {
        try {
            log.warn("chat_stream_error_event code={} message={}", code, message);
            sink.send(EVENT_ERROR, new ChatStreamErrorEvent(code, message));
            sink.complete();
        } catch (RuntimeException exception) {
            log.warn("chat_stream_error_event_failed code={}", code, exception);
            sink.completeWithError(exception);
        }
    }

    private static class ChatStreamTiming {

        private final long startNanos;
        private long userSavedNanos;
        private long contextWaitStartNanos;
        private long contextWaitEndNanos;
        private long assistantPromptBuiltNanos;
        private long firstDeltaNanos;
        private long assistantStreamCompletedNanos;
        private long assistantSavedNanos;
        private long toolCompletedNanos;
        private long toolJoinedNanos;

        static ChatStreamTiming start() {
            return new ChatStreamTiming(System.nanoTime());
        }

        private ChatStreamTiming(long startNanos) {
            this.startNanos = startNanos;
        }

        void markUserSaved() {
            userSavedNanos = System.nanoTime();
        }

        void markContextWaitStarted() {
            contextWaitStartNanos = System.nanoTime();
        }

        void markContextWaitCompleted() {
            contextWaitEndNanos = System.nanoTime();
        }

        void markAssistantPromptBuilt() {
            assistantPromptBuiltNanos = System.nanoTime();
        }

        void markFirstDelta() {
            firstDeltaNanos = System.nanoTime();
        }

        void markAssistantStreamCompleted() {
            assistantStreamCompletedNanos = System.nanoTime();
        }

        void markAssistantSaved() {
            assistantSavedNanos = System.nanoTime();
        }

        void markToolCompleted() {
            toolCompletedNanos = System.nanoTime();
        }

        void markToolJoined() {
            toolJoinedNanos = System.nanoTime();
        }

        long userSaveMs() {
            return elapsedMs(startNanos, userSavedNanos);
        }

        long contextWaitMs() {
            return elapsedMs(contextWaitStartNanos, contextWaitEndNanos);
        }

        long assistantPromptMs() {
            return elapsedMs(contextWaitEndNanos, assistantPromptBuiltNanos);
        }

        long firstDeltaMs() {
            return elapsedMs(startNanos, firstDeltaNanos);
        }

        long assistantStreamMs() {
            return elapsedMs(firstDeltaNanos, assistantStreamCompletedNanos);
        }

        long assistantSaveMs() {
            return elapsedMs(assistantStreamCompletedNanos, assistantSavedNanos);
        }

        long toolTotalMs() {
            return elapsedMs(startNanos, toolCompletedNanos);
        }

        long toolJoinWaitMs() {
            return elapsedMs(assistantSavedNanos, toolJoinedNanos);
        }

        long totalMs() {
            return elapsedMs(startNanos, toolJoinedNanos);
        }

        private long elapsedMs(long fromNanos, long toNanos) {
            if (fromNanos == 0L || toNanos == 0L) {
                return -1L;
            }
            return TimeUnit.NANOSECONDS.toMillis(toNanos - fromNanos);
        }
    }
}
