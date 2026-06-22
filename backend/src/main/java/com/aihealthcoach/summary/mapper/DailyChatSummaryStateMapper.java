package com.aihealthcoach.summary.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.aihealthcoach.summary.entity.DailyChatSummaryState;

@Mapper
public interface DailyChatSummaryStateMapper {
    void upsert(DailyChatSummaryState state);
}
