package com.aihealthcoach.summary.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextCacheEntry;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextVersion;
import com.aihealthcoach.summary.entity.DailyChatSummary;

@Mapper
public interface DailyChatSummaryMapper {
    void upsert(DailyChatSummary summary);

    List<DailyChatSummaryContextResponse> findFreshSummariesBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<DailyChatSummaryContextCacheEntry> findFreshSummaryCacheEntriesBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    List<DailyChatSummaryContextVersion> findFreshSummaryVersionsBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
