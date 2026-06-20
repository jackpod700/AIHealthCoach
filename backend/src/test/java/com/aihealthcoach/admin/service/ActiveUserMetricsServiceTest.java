package com.aihealthcoach.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ActiveUserMetricsServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T05:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void activeUsers5mCountsDistinctAuthenticatedUsers() {
        ActiveUserMetricsService service = new ActiveUserMetricsService(CLOCK);

        service.record(1L);
        service.record(1L);
        service.record(2L);
        service.record(null);

        assertThat(service.activeUsers5m()).isEqualTo(2);
    }
}
