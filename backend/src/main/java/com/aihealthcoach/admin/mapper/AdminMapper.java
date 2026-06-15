package com.aihealthcoach.admin.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aihealthcoach.admin.dto.AiUsageSummary;
import com.aihealthcoach.admin.entity.AiUsageLog;

@Mapper
public interface AdminMapper {
    void insertAiUsageLog(AiUsageLog aiUsageLog);

    AiUsageSummary summarizeAiUsageToday();

    long countUsers();

    long countTodaySignups();
}
