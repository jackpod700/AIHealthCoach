package com.aihealthcoach.summary.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.summary.entity.DailyChatSummaryChangeSource;
import com.aihealthcoach.summary.entity.DailyChatSummaryState;
import com.aihealthcoach.summary.mapper.DailyChatSummaryStateMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DailyChatSummaryStateServiceImpl implements DailyChatSummaryStateService {

    private static final int REGENERATION_WINDOW_DAYS = 7;

    private final DailyChatSummaryStateMapper dailyChatSummaryStateMapper;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markChanged(Long userId, LocalDate summaryDate, DailyChatSummaryChangeSource source) {
        upsertChanged(userId, summaryDate, source, null);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void markDailyGoalChanged(Long userId, LocalDate summaryDate, String dailyGoalSnapshotPayload) {
        upsertChanged(userId, summaryDate, DailyChatSummaryChangeSource.DAILY_GOAL, dailyGoalSnapshotPayload);
    }

    private void upsertChanged(
            Long userId,
            LocalDate summaryDate,
            DailyChatSummaryChangeSource source,
            String dailyGoalSnapshotPayload
    ) {
        LocalDate oldestRegenerableDate = LocalDate.now(clock).minusDays(REGENERATION_WINDOW_DAYS - 1);
        if (summaryDate.isBefore(oldestRegenerableDate)) {
            return;
        }

        dailyChatSummaryStateMapper.upsertChanged(DailyChatSummaryState.builder()
                .userId(userId)
                .summaryDate(summaryDate)
                .changedSources(source.name())
                .dailyGoalSnapshotPayload(dailyGoalSnapshotPayload)
                .build());
    }
}
