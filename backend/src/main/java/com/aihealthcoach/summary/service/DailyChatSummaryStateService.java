package com.aihealthcoach.summary.service;

import java.time.LocalDate;

import com.aihealthcoach.summary.entity.DailyChatSummaryChangeSource;

public interface DailyChatSummaryStateService {
    void markChanged(Long userId, LocalDate summaryDate, DailyChatSummaryChangeSource source);

    void markDailyGoalChanged(Long userId, LocalDate summaryDate, String dailyGoalSnapshotPayload);
}
