package com.aihealthcoach.admin.entity;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AiUsageLog {
    private Long id;
    private Long userId;
    private String requestType;
    private String model;
    private Long latencyMs;
    private Boolean success;
    private String errorMessage;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private OffsetDateTime createdAt;
}
