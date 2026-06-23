package com.aihealthcoach.chat.service;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Qualifier;
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
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.summary.mapper.DailyChatSummaryMapper;
import com.aihealthcoach.summary.service.DailySummaryContextCache;
import com.aihealthcoach.summary.service.DailyChatSummaryService;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;
import com.aihealthcoach.user.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ContextBuilderImpl implements ContextBuilder {

    private static final int RECENT_TURN_LIMIT = 10;
    private static final int ACTIVE_MEMORY_LIMIT = 10;
    private static final int RECENT_DAILY_SUMMARY_DAYS = 6;

    private final UserService userService;
    private final DailyGoalService dailyGoalService;
    private final MealService mealService;
    private final ExerciseService exerciseService;
    private final ChatService chatService;
    private final UserMemoryService userMemoryService;
    private final DailyChatSummaryService dailyChatSummaryService;
    private final DailyChatSummaryMapper dailyChatSummaryMapper;
    private final DailySummaryContextCache dailySummaryContextCache;
    private final Executor contextExecutor;

    public ContextBuilderImpl(
            UserService userService,
            DailyGoalService dailyGoalService,
            MealService mealService,
            ExerciseService exerciseService,
            ChatService chatService,
            UserMemoryService userMemoryService,
            DailyChatSummaryService dailyChatSummaryService,
            DailyChatSummaryMapper dailyChatSummaryMapper,
            DailySummaryContextCache dailySummaryContextCache,
            @Qualifier("chatContextTaskExecutor") Executor contextExecutor
    ) {
        this.userService = userService;
        this.dailyGoalService = dailyGoalService;
        this.mealService = mealService;
        this.exerciseService = exerciseService;
        this.chatService = chatService;
        this.userMemoryService = userMemoryService;
        this.dailyChatSummaryService = dailyChatSummaryService;
        this.dailyChatSummaryMapper = dailyChatSummaryMapper;
        this.dailySummaryContextCache = dailySummaryContextCache;
        this.contextExecutor = contextExecutor;
    }

    @Override
    public UserChatContext build(Long userId, LocalDate contextDate) {
        long totalStartedAt = System.nanoTime();

        long refreshMs = measure(() -> refreshDailySummaries(userId));
        CompletableFuture<TimedValue<UserProfileResponse>> profileFuture = measureAsync(
                () -> userService.findProfileIfExists(userId)
        );
        CompletableFuture<TimedValue<DailyGoalResponse>> dailyGoalFuture = measureAsync(
                () -> dailyGoalService.findCurrentGoalIfExists(userId)
        );
        CompletableFuture<TimedValue<DailyMealResponse>> dailyMealsFuture = measureAsync(
                () -> mealService.findDailyMeals(userId, contextDate)
        );
        CompletableFuture<TimedValue<List<ExerciseRecordResponse>>> dailyExercisesFuture = measureAsync(
                () -> exerciseService.findExerciseRecordsByDate(userId, contextDate)
        );
        CompletableFuture<TimedValue<List<DailyChatSummaryContextResponse>>> recentDailySummariesFuture = measureAsync(
                () -> findRecentDailySummaries(userId, contextDate)
        );
        CompletableFuture<TimedValue<List<ChatMessageResponse>>> recentTurnsFuture = measureAsync(
                () -> chatService.findRecentMessages(userId, RECENT_TURN_LIMIT)
        );
        CompletableFuture<TimedValue<List<UserMemoryResponse>>> activeMemoriesFuture = measureAsync(
                () -> userMemoryService.findActiveMemories(userId, ACTIVE_MEMORY_LIMIT)
        );

        TimedValue<UserProfileResponse> profile = join(profileFuture);
        TimedValue<DailyGoalResponse> dailyGoal = join(dailyGoalFuture);
        TimedValue<DailyMealResponse> dailyMeals = join(dailyMealsFuture);
        TimedValue<List<ExerciseRecordResponse>> dailyExercises = join(dailyExercisesFuture);
        TimedValue<List<DailyChatSummaryContextResponse>> recentDailySummaries = join(recentDailySummariesFuture);
        TimedValue<List<ChatMessageResponse>> recentTurns = join(recentTurnsFuture);
        TimedValue<List<UserMemoryResponse>> activeMemories = join(activeMemoriesFuture);

        log.info(
                "chat_context_build_timing user_id={} refresh_ms={} profile_ms={} daily_goal_ms={} daily_meals_ms={} daily_exercises_ms={} daily_summaries_ms={} recent_turns_ms={} active_memories_ms={} total_ms={}",
                userId,
                refreshMs,
                profile.elapsedMs(),
                dailyGoal.elapsedMs(),
                dailyMeals.elapsedMs(),
                dailyExercises.elapsedMs(),
                recentDailySummaries.elapsedMs(),
                recentTurns.elapsedMs(),
                activeMemories.elapsedMs(),
                elapsedMs(totalStartedAt)
        );

        return new UserChatContext(
                profile.value(),
                dailyGoal.value(),
                dailyMeals.value(),
                dailyExercises.value(),
                recentDailySummaries.value(),
                recentTurns.value(),
                activeMemories.value()
        );
    }

    private void refreshDailySummaries(Long userId) {
        try {
            dailyChatSummaryService.refreshForUser(userId);
        } catch (RuntimeException exception) {
            log.warn("Failed to refresh daily chat summaries for context. user_id={}", userId, exception);
        }
    }

    private List<DailyChatSummaryContextResponse> findRecentDailySummaries(Long userId, LocalDate contextDate) {
        LocalDate from = contextDate.minusDays(RECENT_DAILY_SUMMARY_DAYS);
        LocalDate to = contextDate.minusDays(1);
        return dailySummaryContextCache.getOrLoad(
                userId,
                from,
                to,
                () -> dailyChatSummaryMapper.findFreshSummariesBetween(userId, from, to)
        );
    }

    private long measure(Runnable operation) {
        long startedAt = System.nanoTime();
        operation.run();
        return elapsedMs(startedAt);
    }

    private <T> TimedValue<T> measure(Supplier<T> supplier) {
        long startedAt = System.nanoTime();
        T value = supplier.get();
        return new TimedValue<>(value, elapsedMs(startedAt));
    }

    private <T> CompletableFuture<TimedValue<T>> measureAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(() -> measure(supplier), contextExecutor);
    }

    private <T> T join(CompletableFuture<T> future) {
        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw exception;
        }
    }

    private long elapsedMs(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }

    private record TimedValue<T>(
            T value,
            long elapsedMs
    ) {
    }
}
