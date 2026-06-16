package com.aihealthcoach.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.aihealthcoach.admin.dto.AiUsageSummary;
import com.aihealthcoach.admin.dto.AdminDto.ServerMetricsResponse;
import com.aihealthcoach.admin.dto.AdminDto.TrafficMetricsResponse;
import com.aihealthcoach.admin.mapper.AdminMapper;

class AdminMetricsHistoryServiceTest {

    @Test
    void recordsAndReturnsHistoryPointsWithinRange() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-16T01:00:00Z"));
        AdminDashboardServiceImpl dashboardService = mock(AdminDashboardServiceImpl.class);
        ApiTrafficMetricsService trafficMetricsService = mock(ApiTrafficMetricsService.class);
        AdminMapper adminMapper = mock(AdminMapper.class);

        when(dashboardService.serverMetrics()).thenReturn(
                new ServerMetricsResponse(10.0, 50L, 100L, 25L, 100L, 70.0, 3600L)
        );
        when(trafficMetricsService.snapshot()).thenReturn(
                new TrafficMetricsResponse(1L, 5L, 20L, 80.0, 1L, 0L)
        );
        when(adminMapper.summarizeAiUsageSince(org.mockito.ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(
                new AiUsageSummary(2L, 1L, 1L, 1200.0, 100L, 50L, 150L)
        );

        AdminMetricsHistoryService service = new AdminMetricsHistoryService(
                clock,
                dashboardService,
                trafficMetricsService,
                adminMapper
        );

        service.recordSnapshot();

        var history = service.history(60);

        assertThat(history.rangeMinutes()).isEqualTo(60);
        assertThat(history.points()).hasSize(1);
        assertThat(history.points().getFirst().ramUsagePercent()).isEqualTo(50.0);
        assertThat(history.points().getFirst().jvmHeapUsagePercent()).isEqualTo(25.0);
        assertThat(history.points().getFirst().failureRate5m()).isEqualTo(28.57142857142857);
        assertThat(history.points().getFirst().totalTokens5m()).isEqualTo(150L);
        assertThat(history.points().getFirst().totalTokensDelta()).isEqualTo(150L);
    }

    @Test
    void evictsPointsOlderThanOneHour() {
        MutableClock clock = new MutableClock(Instant.parse("2026-06-16T01:00:00Z"));
        AdminDashboardServiceImpl dashboardService = mock(AdminDashboardServiceImpl.class);
        ApiTrafficMetricsService trafficMetricsService = mock(ApiTrafficMetricsService.class);
        AdminMapper adminMapper = mock(AdminMapper.class);

        when(dashboardService.serverMetrics()).thenReturn(
                new ServerMetricsResponse(10.0, 50L, 100L, 25L, 100L, 70.0, 3600L)
        );
        when(trafficMetricsService.snapshot()).thenReturn(
                new TrafficMetricsResponse(0L, 0L, 0L, 0.0, 0L, 0L)
        );
        when(adminMapper.summarizeAiUsageSince(org.mockito.ArgumentMatchers.any(OffsetDateTime.class))).thenReturn(AiUsageSummary.empty());

        AdminMetricsHistoryService service = new AdminMetricsHistoryService(
                clock,
                dashboardService,
                trafficMetricsService,
                adminMapper
        );

        service.recordSnapshot();
        clock.setInstant(Instant.parse("2026-06-16T02:01:00Z"));
        service.recordSnapshot();

        assertThat(service.history(60).points()).hasSize(1);
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone = ZoneId.of("Asia/Seoul");

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
