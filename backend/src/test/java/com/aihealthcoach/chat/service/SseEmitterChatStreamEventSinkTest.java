package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class SseEmitterChatStreamEventSinkTest {

    @Test
    void completeWithErrorSuppressesAsyncErrorDispatch() {
        SseEmitter emitter = org.mockito.Mockito.mock(SseEmitter.class);
        SseEmitterChatStreamEventSink sink = new SseEmitterChatStreamEventSink(emitter);

        sink.completeWithError(new IllegalStateException("client disconnected"));

        verify(emitter).complete();
        verify(emitter, never()).completeWithError(any(Throwable.class));
    }

    @Test
    void sendFailureIsEscalatedToOrchestratorForPolicyHandling() throws Exception {
        SseEmitter emitter = org.mockito.Mockito.mock(SseEmitter.class);
        doThrow(new IOException("broken pipe")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        SseEmitterChatStreamEventSink sink = new SseEmitterChatStreamEventSink(emitter);

        assertThatThrownBy(() -> sink.send("delta", "partial response"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to send chat stream event");

        verify(emitter, never()).completeWithError(any(Throwable.class));
    }
}
