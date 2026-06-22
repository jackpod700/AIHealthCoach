package com.aihealthcoach.admin.service;

import java.util.concurrent.TimeUnit;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Service
public class AiUsageMetricsService {

    private final MeterRegistry meterRegistry;
    private final String configuredModel;

    public AiUsageMetricsService(
            MeterRegistry meterRegistry,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String configuredModel
    ) {
        this.meterRegistry = meterRegistry;
        this.configuredModel = configuredModel;
    }

    public void recordSuccess(String requestType, long latencyMs, ChatResponse response) {
        record(requestType, latencyMs, "success", response);
    }

    public void recordFailure(String requestType, long latencyMs) {
        record(requestType, latencyMs, "failure", null);
    }

    private void record(String requestType, long latencyMs, String result, ChatResponse response) {
        Usage usage = response == null || response.getMetadata() == null
                ? null
                : response.getMetadata().getUsage();
        String model = modelName(response);

        Counter.builder("ai_requests")
                .description("Total AI requests")
                .tag("request_type", requestType)
                .tag("model", model)
                .tag("result", result)
                .register(meterRegistry)
                .increment();

        Timer.builder("ai_latency")
                .description("AI request latency")
                .tag("request_type", requestType)
                .tag("model", model)
                .tag("result", result)
                .register(meterRegistry)
                .record(latencyMs, TimeUnit.MILLISECONDS);

        if (usage == null) {
            return;
        }

        recordTokenMetric(requestType, model, "input", usage.getPromptTokens());
        recordTokenMetric(requestType, model, "output", usage.getCompletionTokens());
        recordTokenMetric(requestType, model, "total", usage.getTotalTokens());
    }

    private void recordTokenMetric(String requestType, String model, String tokenType, Integer tokens) {
        if (tokens == null) {
            return;
        }

        Counter.builder("ai_tokens")
                .description("Total AI tokens")
                .tag("request_type", requestType)
                .tag("model", model)
                .tag("token_type", tokenType)
                .register(meterRegistry)
                .increment(tokens);
    }

    private String modelName(ChatResponse response) {
        if (response != null && response.getMetadata() != null && response.getMetadata().getModel() != null) {
            return response.getMetadata().getModel();
        }
        return configuredModel;
    }
}
