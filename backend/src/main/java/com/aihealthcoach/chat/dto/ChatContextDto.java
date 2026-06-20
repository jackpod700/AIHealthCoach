package com.aihealthcoach.chat.dto;

import java.util.List;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;

public class ChatContextDto {

    public record UserChatContext(
            UserProfileResponse profile,
            DailyGoalResponse dailyGoal,
            DailyMealResponse dailyMeals,
            List<ExerciseRecordResponse> dailyExercises,
            List<ChatMessageResponse> recentTurns
    ) {
        public UserChatContext {
            dailyExercises = dailyExercises == null ? List.of() : List.copyOf(dailyExercises);
            recentTurns = recentTurns == null ? List.of() : List.copyOf(recentTurns);
        }
    }

}
