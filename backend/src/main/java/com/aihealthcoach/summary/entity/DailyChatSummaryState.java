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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
