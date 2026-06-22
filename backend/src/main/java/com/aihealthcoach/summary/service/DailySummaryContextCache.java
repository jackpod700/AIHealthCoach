package com.aihealthcoach.summary.service;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextCacheEntry;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextVersion;

public interface DailySummaryContextCache {

    List<DailyChatSummaryContextResponse> getOrLoad(
            Long userId,
            LocalDate from,
            LocalDate to,
            Supplier<List<DailyChatSummaryContextResponse>> loader
    );

    List<DailyChatSummaryContextResponse> getOrLoad(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<DailyChatSummaryContextVersion> currentVersions,
            Supplier<List<DailyChatSummaryContextCacheEntry>> loader
    );

    void evictUser(Long userId);

    void clear();
}
