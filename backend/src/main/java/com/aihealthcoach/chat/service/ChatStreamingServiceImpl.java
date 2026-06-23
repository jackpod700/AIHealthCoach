package com.aihealthcoach.chat.service;

import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ChatStreamingServiceImpl implements ChatStreamingService {

    private static final long DEFAULT_TIMEOUT_MILLIS = 0L;

    private final ChatStreamingOrchestrator orchestrator;
    private final Executor executor;

    public ChatStreamingServiceImpl(
            ChatStreamingOrchestrator orchestrator,
            @Qualifier("applicationTaskExecutor") Executor executor
    ) {
        this.orchestrator = orchestrator;
        this.executor = executor;
    }

    @Override
    public SseEmitter stream(Long userId, ChatMessageRequest request) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT_MILLIS);
        emitter.onCompletion(() -> log.info("chat_stream_emitter_completed user_id={}", userId));
        emitter.onTimeout(() -> log.warn("chat_stream_emitter_timed_out user_id={}", userId));
        emitter.onError(error -> log.warn("chat_stream_emitter_error user_id={}", userId, error));

        ChatStreamEventSink sink = new SseEmitterChatStreamEventSink(emitter);
        executor.execute(() -> {
            try {
                orchestrator.stream(userId, request, sink);
            } catch (RuntimeException exception) {
                log.error("chat_stream_unhandled_failure user_id={}", userId, exception);
                sink.completeWithError(exception);
            }
        });
        return emitter;
    }
}
