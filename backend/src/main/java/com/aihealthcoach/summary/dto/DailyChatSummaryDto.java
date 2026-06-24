package com.aihealthcoach.summary.dto;

import java.util.List;

import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.weight.dto.WeightRecordDto.WeightRecordResponse;

public class DailyChatSummaryDto {

    public record DailyChatSummaryContextResponse(
            java.time.LocalDate summaryDate,
            String content
    ) {
    }

    public record DailyChatSummaryContextCacheEntry(
            java.time.LocalDate summaryDate,
            String content,
            Long sourceVersion
    ) {
    }

    public record DailyChatSummaryContextVersion(
            java.time.LocalDate summaryDate,
            Long sourceVersion
    ) {
    }

    public record DailyChatSummarySourceData(
            Long userId,
            java.time.LocalDate summaryDate,
            String changedSources,
            String dailyGoalSnapshotPayload,
            DailyGoalProgressResponse dailyGoalProgress,
            List<ChatMessage> chatMessages,
            DailyMealResponse meals,
            List<ExerciseRecordResponse> exercises,
            WeightRecordResponse weightRecord
    ) {
        public DailyChatSummarySourceData {
            chatMessages = chatMessages == null ? List.of() : List.copyOf(chatMessages);
            exercises = exercises == null ? List.of() : List.copyOf(exercises);
        }
    }
}
