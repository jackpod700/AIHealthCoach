package com.aihealthcoach.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AiUsageMetricsServiceTest {

    @Test
    void recordsSuccessMetricsWithTokenUsageMetadata() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiUsageMetricsService service = new AiUsageMetricsService(meterRegistry, "configured-model");
        ChatResponse response = mock(ChatResponse.class, RETURNS_DEEP_STUBS);
        Usage usage = mock(Usage.class);

        when(response.getMetadata().getModel()).thenReturn("gpt-4o-mini");
        when(response.getMetadata().getUsage()).thenReturn(usage);
        when(usage.getPromptTokens()).thenReturn(11);
        when(usage.getCompletionTokens()).thenReturn(22);
        when(usage.getTotalTokens()).thenReturn(33);

        service.recordSuccess("TEXT_CHAT", 123L, response);

        assertThat(meterRegistry.counter("ai_requests",
                "request_type", "TEXT_CHAT",
                "model", "gpt-4o-mini",
                "result", "success").count()).isEqualTo(1.0);
        assertThat(meterRegistry.counter("ai_tokens",
                "request_type", "TEXT_CHAT",
                "model", "gpt-4o-mini",
                "token_type", "total").count()).isEqualTo(33.0);
        assertThat(meterRegistry.find("ai_latency")
                .tag("request_type", "TEXT_CHAT")
                .tag("model", "gpt-4o-mini")
                .tag("result", "success")
                .timer()).isNotNull();
    }

    @Test
    void usesConfiguredModelWhenResponseModelIsMissing() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        AiUsageMetricsService service = new AiUsageMetricsService(meterRegistry, "configured-model");

        service.recordFailure("IMAGE_MEAL", 456L);

        assertThat(meterRegistry.counter("ai_requests",
                "request_type", "IMAGE_MEAL",
                "model", "configured-model",
                "result", "failure").count()).isEqualTo(1.0);
    }
}
