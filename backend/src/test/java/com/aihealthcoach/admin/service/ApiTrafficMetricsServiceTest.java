package com.aihealthcoach.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class ApiTrafficMetricsServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-15T05:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void snapshotAggregatesRecentRequestMetrics() {
        ApiTrafficMetricsService service = new ApiTrafficMetricsService(CLOCK);

        service.record("/api/chat/messages", 200, 100, 1L);
        service.record("/api/meals", 404, 200, 1L);
        service.record("/api/admin/dashboard", 500, 300, 2L);

        var snapshot = service.snapshot();

        assertThat(snapshot.requestCount1m()).isEqualTo(3);
        assertThat(snapshot.requestCount5m()).isEqualTo(3);
        assertThat(snapshot.requestCount1h()).isEqualTo(3);
        assertThat(snapshot.averageResponseMs5m()).isEqualTo(200.0);
        assertThat(snapshot.clientErrorCount5m()).isEqualTo(1);
        assertThat(snapshot.serverErrorCount5m()).isEqualTo(1);
        assertThat(service.activeUsers5m()).isEqualTo(2);
    }
}
