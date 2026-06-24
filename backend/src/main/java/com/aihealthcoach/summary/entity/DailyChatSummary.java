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
public class DailyChatSummary {
    private Long id;
    private Long userId;
    private LocalDate summaryDate;
    private String content;
    private Long sourceVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
