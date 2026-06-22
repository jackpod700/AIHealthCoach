package com.aihealthcoach.summary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.summary.entity.DailyChatSummaryState;
import com.aihealthcoach.summary.entity.DailyChatSummaryChangeSource;
import com.aihealthcoach.summary.mapper.DailyChatSummaryStateMapper;

@ExtendWith(MockitoExtension.class)
class DailyChatSummaryStateServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-20T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private DailyChatSummaryStateMapper dailyChatSummaryStateMapper;

    private DailyChatSummaryStateServiceImpl stateService;

    @BeforeEach
    void setUp() {
        stateService = new DailyChatSummaryStateServiceImpl(dailyChatSummaryStateMapper, CLOCK);
    }

    @Test
    void markChangedUpsertsStateForTodayAndFutureDates() {
        stateService.markChanged(1L, LocalDate.of(2026, 6, 20), DailyChatSummaryChangeSource.CHAT);
        stateService.markChanged(1L, LocalDate.of(2026, 6, 22), DailyChatSummaryChangeSource.MEAL);

        ArgumentCaptor<DailyChatSummaryState> captor = ArgumentCaptor.forClass(DailyChatSummaryState.class);
        verify(dailyChatSummaryStateMapper, times(2)).upsertChanged(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyChatSummaryState::getSummaryDate)
                .containsExactly(LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 22));
    }

    @Test
    void markChangedIgnoresDatesOlderThanSevenDays() {
        stateService.markChanged(1L, LocalDate.of(2026, 6, 13), DailyChatSummaryChangeSource.CHAT);

        verify(dailyChatSummaryStateMapper, never()).upsertChanged(any());
    }

    @Test
    void markChangedKeepsTheUserAndSummaryDateInTheState() {
        stateService.markChanged(7L, LocalDate.of(2026, 6, 14), DailyChatSummaryChangeSource.WEIGHT);

        ArgumentCaptor<DailyChatSummaryState> captor = ArgumentCaptor.forClass(DailyChatSummaryState.class);
        verify(dailyChatSummaryStateMapper).upsertChanged(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getSummaryDate()).isEqualTo(LocalDate.of(2026, 6, 14));
        assertThat(captor.getValue().getChangedSources()).isEqualTo("WEIGHT");
    }

    @Test
    void markDailyGoalChangedStoresSnapshotPayload() {
        stateService.markDailyGoalChanged(7L, LocalDate.of(2026, 6, 14), "{\"goalType\":\"WEIGHT_LOSS\"}");

        ArgumentCaptor<DailyChatSummaryState> captor = ArgumentCaptor.forClass(DailyChatSummaryState.class);
        verify(dailyChatSummaryStateMapper).upsertChanged(captor.capture());
        assertThat(captor.getValue().getChangedSources()).isEqualTo("DAILY_GOAL");
        assertThat(captor.getValue().getDailyGoalSnapshotPayload()).isEqualTo("{\"goalType\":\"WEIGHT_LOSS\"}");
    }
}
