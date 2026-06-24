package com.aihealthcoach.summary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.mapper.ChatMapper;
import com.aihealthcoach.dailygoal.service.DailyGoalService;
import com.aihealthcoach.exercise.service.ExerciseService;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.service.MealService;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummarySourceData;
import com.aihealthcoach.summary.entity.DailyChatSummary;
import com.aihealthcoach.summary.entity.DailyChatSummaryState;
import com.aihealthcoach.summary.mapper.DailyChatSummaryMapper;
import com.aihealthcoach.summary.mapper.DailyChatSummaryStateMapper;
import com.aihealthcoach.weight.service.WeightRecordService;

@ExtendWith(MockitoExtension.class)
class DailyChatSummaryServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-22T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final LocalDate SUMMARY_DATE = LocalDate.of(2026, 6, 21);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 22, 12, 0);
    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 6, 22);
    private static final LocalDate OLDEST_REGENERABLE_DATE = LocalDate.of(2026, 6, 16);
    private static final Long USER_ID = 1L;

    @Mock
    private DailyChatSummaryStateMapper stateMapper;
    @Mock
    private DailyChatSummaryMapper summaryMapper;
    @Mock
    private ChatMapper chatMapper;
    @Mock
    private DailyGoalService dailyGoalService;
    @Mock
    private MealService mealService;
    @Mock
    private ExerciseService exerciseService;
    @Mock
    private WeightRecordService weightRecordService;
    @Mock
    private DailyChatSummaryGenerator generator;

    private DailyChatSummaryServiceImpl summaryService;

    @BeforeEach
    void setUp() {
        summaryService = new DailyChatSummaryServiceImpl(
                stateMapper,
                summaryMapper,
                chatMapper,
                dailyGoalService,
                mealService,
                exerciseService,
                weightRecordService,
                generator,
                new ImmediateTransactionManager(),
                CLOCK
        );
    }

    @Test
    void processPendingBatchClaimsUpToFiftyRowsAndStoresSummaryOnSuccess() {
        DailyChatSummaryState claim = claim();
        when(stateMapper.claimBatch(
                eq(50),
                anyString(),
                eq(NOW),
                eq(NOW.minusMinutes(10)),
                eq(NOW.minusMinutes(5)),
                eq(CURRENT_DATE),
                eq(OLDEST_REGENERABLE_DATE)
        )).thenReturn(List.of(claim));
        stubSourceData();
        when(generator.generate(any(DailyChatSummarySourceData.class))).thenReturn("좋은 하루 요약");
        when(stateMapper.markFresh(USER_ID, SUMMARY_DATE, "token-1", 3L)).thenReturn(1);

        int processed = summaryService.processPendingBatch();

        assertThat(processed).isEqualTo(1);
        ArgumentCaptor<DailyChatSummarySourceData> sourceCaptor =
                ArgumentCaptor.forClass(DailyChatSummarySourceData.class);
        verify(generator).generate(sourceCaptor.capture());
        assertThat(sourceCaptor.getValue().dailyGoalSnapshotPayload())
                .isEqualTo("{\"goalType\":\"WEIGHT_LOSS\"}");
        ArgumentCaptor<DailyChatSummary> summaryCaptor = ArgumentCaptor.forClass(DailyChatSummary.class);
        verify(summaryMapper).upsert(summaryCaptor.capture());
        assertThat(summaryCaptor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(summaryCaptor.getValue().getSummaryDate()).isEqualTo(SUMMARY_DATE);
        assertThat(summaryCaptor.getValue().getContent()).isEqualTo("좋은 하루 요약");
        assertThat(summaryCaptor.getValue().getSourceVersion()).isEqualTo(3L);
    }

    @Test
    void refreshForUserClaimsAtMostTwoRowsForThatUser() {
        when(stateMapper.claimForUser(
                eq(USER_ID),
                eq(2),
                anyString(),
                eq(NOW),
                eq(NOW.minusMinutes(10)),
                eq(NOW.minusMinutes(5)),
                eq(CURRENT_DATE),
                eq(OLDEST_REGENERABLE_DATE)
        )).thenReturn(List.of());

        int processed = summaryService.refreshForUser(USER_ID);

        assertThat(processed).isZero();
    }

    @Test
    void successDoesNotStoreSummaryWhenSourceVersionChangedDuringGeneration() {
        DailyChatSummaryState claim = claim();
        when(stateMapper.claimBatch(
                eq(50),
                anyString(),
                eq(NOW),
                eq(NOW.minusMinutes(10)),
                eq(NOW.minusMinutes(5)),
                eq(CURRENT_DATE),
                eq(OLDEST_REGENERABLE_DATE)
        ))
                .thenReturn(List.of(claim));
        stubSourceData();
        when(generator.generate(any(DailyChatSummarySourceData.class))).thenReturn("오래된 요약");
        when(stateMapper.markFresh(USER_ID, SUMMARY_DATE, "token-1", 3L)).thenReturn(0);

        summaryService.processPendingBatch();

        verify(summaryMapper, never()).upsert(any());
        verify(stateMapper).releaseStaleIfChanged(USER_ID, SUMMARY_DATE, "token-1", 3L);
    }

    @Test
    void generationFailureMarksFailedWithFirstRetryDelay() {
        DailyChatSummaryState claim = claim();
        when(stateMapper.claimBatch(
                eq(50),
                anyString(),
                eq(NOW),
                eq(NOW.minusMinutes(10)),
                eq(NOW.minusMinutes(5)),
                eq(CURRENT_DATE),
                eq(OLDEST_REGENERABLE_DATE)
        ))
                .thenReturn(List.of(claim));
        stubSourceData();
        when(generator.generate(any(DailyChatSummarySourceData.class)))
                .thenThrow(new IllegalStateException("provider unavailable"));

        summaryService.processPendingBatch();

        verify(stateMapper).markFailed(
                USER_ID,
                SUMMARY_DATE,
                "token-1",
                3L,
                "IllegalStateException",
                NOW.plusMinutes(1)
        );
        verify(summaryMapper, never()).upsert(any());
    }

    private void stubSourceData() {
        when(dailyGoalService.findProgress(USER_ID, SUMMARY_DATE)).thenReturn(null);
        when(chatMapper.findMessagesByCreatedAtBetween(
                USER_ID,
                SUMMARY_DATE.atStartOfDay(),
                SUMMARY_DATE.plusDays(1).atStartOfDay()
        )).thenReturn(List.of(ChatMessage.builder()
                .userId(USER_ID)
                .role("USER")
                .content("오늘 목표를 바꿨어")
                .createdAt(SUMMARY_DATE.atTime(9, 0))
                .build()));
        when(mealService.findDailyMeals(USER_ID, SUMMARY_DATE))
                .thenReturn(new DailyMealResponse(SUMMARY_DATE, List.of(), null, null, null, null));
        when(exerciseService.findExerciseRecordsByDate(USER_ID, SUMMARY_DATE)).thenReturn(List.of());
        when(weightRecordService.findWeightRecordByDate(USER_ID, SUMMARY_DATE)).thenReturn(null);
    }

    private DailyChatSummaryState claim() {
        return DailyChatSummaryState.builder()
                .userId(USER_ID)
                .summaryDate(SUMMARY_DATE)
                .sourceVersion(3L)
                .status("CLAIMED")
                .claimToken("token-1")
                .claimedAt(NOW)
                .retryCount(0)
                .changedSources("CHAT,DAILY_GOAL")
                .dailyGoalSnapshotPayload("{\"goalType\":\"WEIGHT_LOSS\"}")
                .build();
    }

    private static class ImmediateTransactionManager implements PlatformTransactionManager {

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
