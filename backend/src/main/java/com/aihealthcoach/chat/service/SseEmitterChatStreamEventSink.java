package com.aihealthcoach.chat.service;

import java.io.IOException;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class SseEmitterChatStreamEventSink implements ChatStreamEventSink {

    private final SseEmitter emitter;

    SseEmitterChatStreamEventSink(SseEmitter emitter) {
        this.emitter = emitter;
    }

    @Override
    public void send(String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
        } catch (IOException exception) {
            log.warn("chat_stream_event_send_failed event={}", eventName, exception);
            throw new IllegalStateException("Failed to send chat stream event", exception);
        }
    }

    @Override
    public void complete() {
        emitter.complete();
    }

    @Override
    public void completeWithError(Throwable error) {
        log.warn("chat_stream_complete_with_error_suppressed", error);
        emitter.complete();
    }
}
