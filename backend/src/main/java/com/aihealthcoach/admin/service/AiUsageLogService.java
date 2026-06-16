package com.aihealthcoach.admin.service;

import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.aihealthcoach.admin.entity.AiUsageLog;
import com.aihealthcoach.admin.mapper.AdminMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiUsageLogService {

    private final AdminMapper adminMapper;
    private final String configuredModel;

    public AiUsageLogService(
            AdminMapper adminMapper,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String configuredModel
    ) {
        this.adminMapper = adminMapper;
        this.configuredModel = configuredModel;
    }

    /**
     * 성공한 AI 호출의 사용량을 저장한다.
     * DB 저장이나 metadata 추출 실패가 실제 채팅 응답 실패로 이어지지 않도록 내부에서 예외를 흡수한다.
     */
    public void recordSuccess(Long userId, String requestType, long latencyMs, ChatResponse response) {
        record(userId, requestType, latencyMs, true, null, response);
    }

    /**
     * 실패한 AI 호출도 latency와 에러 메시지를 남긴다.
     * token metadata는 응답을 받지 못한 경우가 대부분이므로 null로 저장된다.
     */
    public void recordFailure(Long userId, String requestType, long latencyMs, Exception exception) {
        record(userId, requestType, latencyMs, false, exception, null);
    }

    private void record(
            Long userId,
            String requestType,
            long latencyMs,
            boolean success,
            Exception exception,
            ChatResponse response
    ) {
        try {
            Usage usage = response == null || response.getMetadata() == null
                    ? null
                    : response.getMetadata().getUsage();

            adminMapper.insertAiUsageLog(AiUsageLog.builder()
                    .userId(userId)
                    .requestType(requestType)
                    .model(modelName(response))
                    .latencyMs(latencyMs)
                    .success(success)
                    .errorMessage(exception == null ? null : truncate(exception.getMessage(), 1000))
                    .inputTokens(toLong(usage == null ? null : usage.getPromptTokens()))
                    .outputTokens(toLong(usage == null ? null : usage.getCompletionTokens()))
                    .totalTokens(toLong(usage == null ? null : usage.getTotalTokens()))
                    .build());
        } catch (Exception loggingException) {
            log.warn("Failed to record AI usage log.", loggingException);
        }
    }

    private String modelName(ChatResponse response) {
        if (response != null && response.getMetadata() != null && response.getMetadata().getModel() != null) {
            return response.getMetadata().getModel();
        }
        return configuredModel;
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
