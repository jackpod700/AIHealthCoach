package com.aihealthcoach.chat.dto;

import java.util.List;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;

public class ChatContextDto {

    public record UserChatContext(
            UserProfileResponse profile,
            DailyGoalResponse dailyGoal,
            DailyMealResponse dailyMeals,
            List<ExerciseRecordResponse> dailyExercises,
            List<DailyChatSummaryContextResponse> recentDailySummaries,
            List<ChatMessageResponse> recentTurns,
            List<UserMemoryResponse> activeMemories
    ) {
        public UserChatContext {
            dailyExercises = dailyExercises == null ? List.of() : List.copyOf(dailyExercises);
            recentDailySummaries = recentDailySummaries == null ? List.of() : List.copyOf(recentDailySummaries);
            recentTurns = recentTurns == null ? List.of() : List.copyOf(recentTurns);
            activeMemories = activeMemories == null ? List.of() : List.copyOf(activeMemories);
        }
    }

}
