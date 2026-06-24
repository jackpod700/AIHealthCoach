package com.aihealthcoach.summary.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.aihealthcoach.chat.mapper.ChatMapper;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.exception.DailyGoalException;
import com.aihealthcoach.dailygoal.service.DailyGoalService;
import com.aihealthcoach.exercise.service.ExerciseService;
import com.aihealthcoach.meal.service.MealService;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummarySourceData;
import com.aihealthcoach.summary.entity.DailyChatSummary;
import com.aihealthcoach.summary.entity.DailyChatSummaryState;
import com.aihealthcoach.summary.mapper.DailyChatSummaryMapper;
import com.aihealthcoach.summary.mapper.DailyChatSummaryStateMapper;
import com.aihealthcoach.weight.service.WeightRecordService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyChatSummaryServiceImpl implements DailyChatSummaryService {

    private static final int SCHEDULER_BATCH_LIMIT = 50;
    private static final int LAZY_USER_LIMIT = 2;
    private static final int REGENERATION_WINDOW_DAYS = 7;
    private static final Duration CLAIM_LEASE = Duration.ofMinutes(10);
    private static final Duration DEFAULT_DEBOUNCE_WINDOW = Duration.ofMinutes(5);

    private final DailyChatSummaryStateMapper stateMapper;
    private final DailyChatSummaryMapper summaryMapper;
    private final ChatMapper chatMapper;
    private final DailyGoalService dailyGoalService;
    private final MealService mealService;
    private final ExerciseService exerciseService;
    private final WeightRecordService weightRecordService;
    private final DailyChatSummaryGenerator generator;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    @Value("${ai.chat.summary.debounce-window-ms:300000}")
    private long debounceWindowMs = DEFAULT_DEBOUNCE_WINDOW.toMillis();

    @Override
    public int processPendingBatch() {
        LocalDateTime currentTime = now();
        return processClaims(stateMapper.claimBatch(
                SCHEDULER_BATCH_LIMIT,
                newClaimToken(),
                currentTime,
                currentTime.minus(CLAIM_LEASE),
                currentTime.minus(debounceWindow()),
                currentDate(),
                oldestRegenerableDate()
        ));
    }

    @Override
    public int refreshForUser(Long userId) {
        LocalDateTime currentTime = now();
        return processClaims(stateMapper.claimForUser(
                userId,
                LAZY_USER_LIMIT,
                newClaimToken(),
                currentTime,
                currentTime.minus(CLAIM_LEASE),
                currentTime.minus(debounceWindow()),
                currentDate(),
                oldestRegenerableDate()
        ));
    }

    @Scheduled(fixedDelayString = "${ai.chat.summary.scheduler.fixed-delay-ms:60000}")
    public void scheduledProcessPendingBatch() {
        processPendingBatch();
    }

    private int processClaims(List<DailyChatSummaryState> claims) {
        int processed = 0;

        for (DailyChatSummaryState claim : claims) {
            processClaim(claim);
            processed++;
        }

        return processed;
    }

    private void processClaim(DailyChatSummaryState claim) {
        try {
            DailyChatSummarySourceData sourceData = loadSourceData(claim);
            String summaryContent = generator.generate(sourceData);
            markSuccess(claim, summaryContent);
        } catch (RuntimeException exception) {
            markFailure(claim, exception);
        }
    }

    private DailyChatSummarySourceData loadSourceData(DailyChatSummaryState claim) {
        LocalDate summaryDate = claim.getSummaryDate();
        LocalDateTime startOfDay = summaryDate.atStartOfDay();
        LocalDateTime nextDay = summaryDate.plusDays(1).atStartOfDay();

        return new DailyChatSummarySourceData(
                claim.getUserId(),
                summaryDate,
                claim.getChangedSources(),
                claim.getDailyGoalSnapshotPayload(),
                findDailyGoalProgressIfExists(claim.getUserId(), summaryDate),
                chatMapper.findMessagesByCreatedAtBetween(claim.getUserId(), startOfDay, nextDay),
                mealService.findDailyMeals(claim.getUserId(), summaryDate),
                exerciseService.findExerciseRecordsByDate(claim.getUserId(), summaryDate),
                weightRecordService.findWeightRecordByDate(claim.getUserId(), summaryDate)
        );
    }

    private DailyGoalProgressResponse findDailyGoalProgressIfExists(Long userId, LocalDate summaryDate) {
        try {
            return dailyGoalService.findProgress(userId, summaryDate);
        } catch (DailyGoalException exception) {
            return null;
        }
    }

    private void markSuccess(DailyChatSummaryState claim, String summaryContent) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            int updated = stateMapper.markFresh(
                    claim.getUserId(),
                    claim.getSummaryDate(),
                    claim.getClaimToken(),
                    claim.getSourceVersion()
            );

            if (updated == 0) {
                stateMapper.releaseStaleIfChanged(
                        claim.getUserId(),
                        claim.getSummaryDate(),
                        claim.getClaimToken(),
                        claim.getSourceVersion()
                );
                return;
            }

            summaryMapper.upsert(DailyChatSummary.builder()
                    .userId(claim.getUserId())
                    .summaryDate(claim.getSummaryDate())
                    .content(summaryContent)
                    .sourceVersion(claim.getSourceVersion())
                    .build());
        });
    }

    private void markFailure(DailyChatSummaryState claim, RuntimeException exception) {
        stateMapper.markFailed(
                claim.getUserId(),
                claim.getSummaryDate(),
                claim.getClaimToken(),
                claim.getSourceVersion(),
                safeFailureCode(exception),
                nextRetryAt(claim)
        );
        stateMapper.releaseStaleIfChanged(
                claim.getUserId(),
                claim.getSummaryDate(),
                claim.getClaimToken(),
                claim.getSourceVersion()
        );
    }

    private LocalDateTime nextRetryAt(DailyChatSummaryState claim) {
        int retryCount = claim.getRetryCount() == null ? 0 : claim.getRetryCount();
        Duration delay = switch (retryCount) {
            case 0 -> Duration.ofMinutes(1);
            case 1 -> Duration.ofMinutes(5);
            case 2 -> Duration.ofMinutes(30);
            default -> Duration.ofHours(6);
        };
        return now().plus(delay);
    }

    private String safeFailureCode(RuntimeException exception) {
        String simpleName = exception.getClass().getSimpleName();
        return simpleName == null || simpleName.isBlank() ? "SUMMARY_GENERATION_FAILED" : simpleName;
    }

    private LocalDate oldestRegenerableDate() {
        return currentDate().minusDays(REGENERATION_WINDOW_DAYS - 1);
    }

    private LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private Duration debounceWindow() {
        return Duration.ofMillis(debounceWindowMs);
    }

    private String newClaimToken() {
        return UUID.randomUUID().toString();
    }
}
