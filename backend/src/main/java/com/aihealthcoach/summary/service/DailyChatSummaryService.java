package com.aihealthcoach.summary.service;

public interface DailyChatSummaryService {
    int processPendingBatch();

    int refreshForUser(Long userId);
}
