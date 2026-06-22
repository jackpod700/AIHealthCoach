package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

@ExtendWith(MockitoExtension.class)
class ContextBuilderImplTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate CONTEXT_DATE = LocalDate.of(2026, 6, 8);

    @Mock
    private UserService userService;
    @Mock
    private DailyGoalService dailyGoalService;
    @Mock
    private MealService mealService;
    @Mock
    private ExerciseService exerciseService;
    @Mock
    private ChatService chatService;
    @Mock
    private UserMemoryService userMemoryService;
    @Mock
    private DailyChatSummaryService dailyChatSummaryService;
    @Mock
    private DailyChatSummaryMapper dailyChatSummaryMapper;
    @Mock
    private DailySummaryContextCache dailySummaryContextCache;

    private ContextBuilderImpl contextBuilder;

    @BeforeEach
    void setUp() {
        contextBuilder = new ContextBuilderImpl(
                userService,
                dailyGoalService,
                mealService,
                exerciseService,
                chatService,
                userMemoryService,
                dailyChatSummaryService,
                dailyChatSummaryMapper,
                dailySummaryContextCache
        );
    }

    @Test
    void buildCollectsRecentStoredTurnsIntoContext() {
        UserProfileResponse profile = UserProfileResponse.builder()
                .userId(USER_ID)
                .currentWeightKg(new BigDecimal("68.4"))
                .build();
        DailyGoalResponse dailyGoal = new DailyGoalResponse("MAINTENANCE", 2000, 300, LocalDateTime.now());
        DailyMealResponse dailyMeals = emptyDailyMeals();
        List<ExerciseRecordResponse> dailyExercises = List.of();
        List<ChatMessageResponse> recentTurns = List.of(
                ChatMessageResponse.builder().role("USER").content("어제 점심은 샐러드였어").build(),
                ChatMessageResponse.builder().role("ASSISTANT").content("기록해둘게요.").build()
        );
        List<UserMemoryResponse> activeMemories = List.of(
                new UserMemoryResponse(1L, "유제품은 피하고 싶어", true, null, null)
        );
        List<DailyChatSummaryContextResponse> recentDailySummaries = List.of(
                new DailyChatSummaryContextResponse(LocalDate.of(2026, 6, 7), "전날은 섭취 목표에 가까웠다.")
        );
        when(userService.findProfileIfExists(USER_ID)).thenReturn(profile);
        when(dailyGoalService.findCurrentGoalIfExists(USER_ID)).thenReturn(dailyGoal);
        when(mealService.findDailyMeals(USER_ID, CONTEXT_DATE)).thenReturn(dailyMeals);
        when(exerciseService.findExerciseRecordsByDate(USER_ID, CONTEXT_DATE)).thenReturn(dailyExercises);
        stubSummaryCacheWithLoader();
        when(dailyChatSummaryMapper.findFreshSummariesBetween(
                USER_ID,
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 7)
        )).thenReturn(recentDailySummaries);
        when(chatService.findRecentMessages(USER_ID, 10)).thenReturn(recentTurns);
        when(userMemoryService.findActiveMemories(USER_ID, 10)).thenReturn(activeMemories);

        UserChatContext context = contextBuilder.build(USER_ID, CONTEXT_DATE);

        assertThat(context.profile()).isEqualTo(profile);
        assertThat(context.dailyGoal()).isEqualTo(dailyGoal);
        assertThat(context.dailyMeals()).isEqualTo(dailyMeals);
        assertThat(context.dailyExercises()).isEmpty();
        assertThat(context.recentDailySummaries()).isEqualTo(recentDailySummaries);
        assertThat(context.recentTurns()).extracting(ChatMessageResponse::content)
                .containsExactly("어제 점심은 샐러드였어", "기록해둘게요.");
        assertThat(context.activeMemories()).extracting(UserMemoryResponse::content)
                .containsExactly("유제품은 피하고 싶어");
        verify(dailyChatSummaryService).refreshForUser(USER_ID);
        verify(dailyChatSummaryMapper).findFreshSummariesBetween(
                USER_ID,
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 7)
        );
        verify(chatService).findRecentMessages(USER_ID, 10);
        verify(userMemoryService).findActiveMemories(USER_ID, 10);
    }

    @Test
    void buildAllowsEmptyOptionalContextSections() {
        when(userService.findProfileIfExists(USER_ID)).thenReturn(null);
        when(dailyGoalService.findCurrentGoalIfExists(USER_ID)).thenReturn(null);
        when(mealService.findDailyMeals(USER_ID, CONTEXT_DATE)).thenReturn(emptyDailyMeals());
        when(exerciseService.findExerciseRecordsByDate(USER_ID, CONTEXT_DATE)).thenReturn(List.of());
        stubSummaryCacheWithLoader();
        when(dailyChatSummaryMapper.findFreshSummariesBetween(
                USER_ID,
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 7)
        )).thenReturn(List.of());
        when(chatService.findRecentMessages(USER_ID, 10)).thenReturn(List.of());
        when(userMemoryService.findActiveMemories(USER_ID, 10)).thenReturn(List.of());

        UserChatContext context = contextBuilder.build(USER_ID, CONTEXT_DATE);

        assertThat(context.profile()).isNull();
        assertThat(context.dailyGoal()).isNull();
        assertThat(context.dailyExercises()).isEmpty();
        assertThat(context.recentDailySummaries()).isEmpty();
        assertThat(context.recentTurns()).isEmpty();
        assertThat(context.activeMemories()).isEmpty();
    }

    @Test
    void buildContinuesWhenDailySummaryRefreshFails() {
        doThrow(new IllegalStateException("summary refresh failed"))
                .when(dailyChatSummaryService)
                .refreshForUser(USER_ID);
        when(userService.findProfileIfExists(USER_ID)).thenReturn(null);
        when(dailyGoalService.findCurrentGoalIfExists(USER_ID)).thenReturn(null);
        when(mealService.findDailyMeals(USER_ID, CONTEXT_DATE)).thenReturn(emptyDailyMeals());
        when(exerciseService.findExerciseRecordsByDate(USER_ID, CONTEXT_DATE)).thenReturn(List.of());
        stubSummaryCacheWithLoader();
        when(dailyChatSummaryMapper.findFreshSummariesBetween(
                USER_ID,
                LocalDate.of(2026, 6, 2),
                LocalDate.of(2026, 6, 7)
        )).thenReturn(List.of());
        when(chatService.findRecentMessages(USER_ID, 10)).thenReturn(List.of());
        when(userMemoryService.findActiveMemories(USER_ID, 10)).thenReturn(List.of());

        UserChatContext context = contextBuilder.build(USER_ID, CONTEXT_DATE);

        assertThat(context.recentDailySummaries()).isEmpty();
        verify(dailyChatSummaryService).refreshForUser(USER_ID);
    }

    @SuppressWarnings("unchecked")
    private void stubSummaryCacheWithLoader() {
        when(dailySummaryContextCache.getOrLoad(
                eq(USER_ID),
                eq(LocalDate.of(2026, 6, 2)),
                eq(LocalDate.of(2026, 6, 7)),
                any(Supplier.class)
        )).thenAnswer(invocation -> {
            Supplier<List<DailyChatSummaryContextResponse>> loader = invocation.getArgument(3);
            return loader.get();
        });
    }

    private DailyMealResponse emptyDailyMeals() {
        return new DailyMealResponse(
                CONTEXT_DATE,
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
    }
}
