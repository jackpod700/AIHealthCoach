package com.aihealthcoach.summary.mapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.summary.entity.DailyChatSummaryState;

@Mapper
public interface DailyChatSummaryStateMapper {
    void upsertChanged(DailyChatSummaryState state);

    List<DailyChatSummaryState> claimBatch(
            @Param("limit") int limit,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now,
            @Param("claimExpiredBefore") LocalDateTime claimExpiredBefore,
            @Param("debounceExpiredBefore") LocalDateTime debounceExpiredBefore,
            @Param("currentDate") LocalDate currentDate,
            @Param("oldestRegenerableDate") LocalDate oldestRegenerableDate
    );

    List<DailyChatSummaryState> claimForUser(
            @Param("userId") Long userId,
            @Param("limit") int limit,
            @Param("claimToken") String claimToken,
            @Param("now") LocalDateTime now,
            @Param("claimExpiredBefore") LocalDateTime claimExpiredBefore,
            @Param("debounceExpiredBefore") LocalDateTime debounceExpiredBefore,
            @Param("currentDate") LocalDate currentDate,
            @Param("oldestRegenerableDate") LocalDate oldestRegenerableDate
    );

    int markFresh(
            @Param("userId") Long userId,
            @Param("summaryDate") LocalDate summaryDate,
            @Param("claimToken") String claimToken,
            @Param("sourceVersion") Long sourceVersion
    );

    int markFailed(
            @Param("userId") Long userId,
            @Param("summaryDate") LocalDate summaryDate,
            @Param("claimToken") String claimToken,
            @Param("sourceVersion") Long sourceVersion,
            @Param("failureCode") String failureCode,
            @Param("nextRetryAt") LocalDateTime nextRetryAt
    );

    int releaseStaleIfChanged(
            @Param("userId") Long userId,
            @Param("summaryDate") LocalDate summaryDate,
            @Param("claimToken") String claimToken,
            @Param("sourceVersion") Long sourceVersion
    );
}
