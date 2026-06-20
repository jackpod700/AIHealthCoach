package com.aihealthcoach.chat.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.dailygoal.service.DailyGoalService;
import com.aihealthcoach.exercise.dto.ExerciseDto.ExerciseRecordResponse;
import com.aihealthcoach.exercise.service.ExerciseService;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.service.MealService;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.memory.service.UserMemoryService;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;
import com.aihealthcoach.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ContextBuilderImpl implements ContextBuilder {

    private static final int RECENT_TURN_LIMIT = 10;
    private static final int ACTIVE_MEMORY_LIMIT = 10;

    private final UserService userService;
    private final DailyGoalService dailyGoalService;
    private final MealService mealService;
    private final ExerciseService exerciseService;
    private final ChatService chatService;
    private final UserMemoryService userMemoryService;

    @Override
    public UserChatContext build(Long userId, LocalDate contextDate) {
        UserProfileResponse profile = userService.findProfileIfExists(userId);
        DailyGoalResponse dailyGoal = dailyGoalService.findCurrentGoalIfExists(userId);
        DailyMealResponse dailyMeals = mealService.findDailyMeals(userId, contextDate);
        List<ExerciseRecordResponse> dailyExercises = exerciseService.findExerciseRecordsByDate(userId, contextDate);
        List<ChatMessageResponse> recentTurns = chatService.findRecentMessages(userId, RECENT_TURN_LIMIT);
        List<UserMemoryResponse> activeMemories = userMemoryService.findActiveMemories(userId, ACTIVE_MEMORY_LIMIT);

        return new UserChatContext(
                profile,
                dailyGoal,
                dailyMeals,
                dailyExercises,
                recentTurns,
                activeMemories
        );
    }
}
