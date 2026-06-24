package com.aihealthcoach.summary.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChatSummaryState {
    private Long userId;
    private LocalDate summaryDate;
    private Long sourceVersion;
    private LocalDateTime sourceUpdatedAt;
    private String status;
    private String claimToken;
    private LocalDateTime claimedAt;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String failureCode;
    private String changedSources;
    private String dailyGoalSnapshotPayload;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
