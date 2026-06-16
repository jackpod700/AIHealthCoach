package com.aihealthcoach.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

import com.aihealthcoach.admin.dto.AiUsageSummary;
import com.aihealthcoach.admin.entity.AiUsageLog;

@Mapper
public interface AdminMapper {
    void insertAiUsageLog(AiUsageLog aiUsageLog);

    AiUsageSummary summarizeAiUsageToday();

    AiUsageSummary summarizeAiUsageSince(@Param("since") OffsetDateTime since);

    long countUsers();

    long countTodaySignups();
}
