package com.aihealthcoach.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageSummary {
    private Long requestCountToday;
    private Long successCountToday;
    private Long failureCountToday;
    private Double averageLatencyMsToday;
    private Long inputTokensToday;
    private Long outputTokensToday;
    private Long totalTokensToday;

    public static AiUsageSummary empty() {
        return new AiUsageSummary(0L, 0L, 0L, 0.0, 0L, 0L, 0L);
    }
}
