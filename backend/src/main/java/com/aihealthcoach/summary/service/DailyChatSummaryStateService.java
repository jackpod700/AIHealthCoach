package com.aihealthcoach.summary.service;

import java.time.LocalDate;

public interface DailyChatSummaryStateService {
    void markChanged(Long userId, LocalDate summaryDate);
}
