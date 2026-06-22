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
        stateService.markChanged(1L, LocalDate.of(2026, 6, 20));
        stateService.markChanged(1L, LocalDate.of(2026, 6, 22));

        ArgumentCaptor<DailyChatSummaryState> captor = ArgumentCaptor.forClass(DailyChatSummaryState.class);
        verify(dailyChatSummaryStateMapper, times(2)).upsert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(DailyChatSummaryState::getSummaryDate)
                .containsExactly(LocalDate.of(2026, 6, 20), LocalDate.of(2026, 6, 22));
    }

    @Test
    void markChangedIgnoresDatesOlderThanSevenDays() {
        stateService.markChanged(1L, LocalDate.of(2026, 6, 13));

        verify(dailyChatSummaryStateMapper, never()).upsert(any());
    }

    @Test
    void markChangedKeepsTheUserAndSummaryDateInTheState() {
        stateService.markChanged(7L, LocalDate.of(2026, 6, 14));

        ArgumentCaptor<DailyChatSummaryState> captor = ArgumentCaptor.forClass(DailyChatSummaryState.class);
        verify(dailyChatSummaryStateMapper).upsert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getSummaryDate()).isEqualTo(LocalDate.of(2026, 6, 14));
    }
}
