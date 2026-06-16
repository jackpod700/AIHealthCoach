package com.aihealthcoach.admin.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.aihealthcoach.admin.dto.AiUsageSummary;
import com.aihealthcoach.admin.dto.AdminDto.AdminDashboardHistoryResponse;
import com.aihealthcoach.admin.dto.AdminDto.AdminMetricsHistoryPoint;
import com.aihealthcoach.admin.dto.AdminDto.ServerMetricsResponse;
import com.aihealthcoach.admin.dto.AdminDto.TrafficMetricsResponse;
import com.aihealthcoach.admin.mapper.AdminMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminMetricsHistoryService {

    private static final int DEFAULT_RANGE_MINUTES = 60;
    private static final int MAX_RANGE_MINUTES = 60;
    private static final int SAMPLE_RATE_MS = 5_000;

    private final Clock clock;
    private final AdminDashboardServiceImpl adminDashboardService;
    private final ApiTrafficMetricsService apiTrafficMetricsService;
    private final AdminMapper adminMapper;
    private final Deque<AdminMetricsHistoryPoint> points = new ConcurrentLinkedDeque<>();

    @Scheduled(fixedRate = SAMPLE_RATE_MS)
    public void recordSnapshot() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        ServerMetricsResponse server = adminDashboardService.serverMetrics();
        TrafficMetricsResponse traffic = apiTrafficMetricsService.snapshot();
        AiUsageSummary ai = adminMapper.summarizeAiUsageSince(now.minusMinutes(5));
        if (ai == null) {
            ai = AiUsageSummary.empty();
        }
        long totalTokens5m = safe(ai.getTotalTokensToday());

        points.addLast(new AdminMetricsHistoryPoint(
                now,
                server.cpuUsagePercent(),
                percent(server.systemMemoryUsedMb(), server.systemMemoryTotalMb()),
                server.diskUsagePercent(),
                percent(server.jvmMemoryUsedMb(), server.jvmMemoryMaxMb()),
                traffic.requestCount1m(),
                traffic.requestCount5m(),
                traffic.requestCount1h(),
                traffic.averageResponseMs5m(),
                ai.getAverageLatencyMsToday() == null ? 0.0 : ai.getAverageLatencyMsToday(),
                failureRate(traffic, ai),
                totalTokens5m,
                totalTokenDelta(totalTokens5m)
        ));

        evictOldPoints(now);
    }

    public AdminDashboardHistoryResponse history(Integer rangeMinutes) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        int normalizedRangeMinutes = normalizeRangeMinutes(rangeMinutes);
        OffsetDateTime cutoff = now.minusMinutes(normalizedRangeMinutes);
        evictOldPoints(now);

        List<AdminMetricsHistoryPoint> filteredPoints = new ArrayList<>();
        for (AdminMetricsHistoryPoint point : points) {
            if (!point.timestamp().isBefore(cutoff)) {
                filteredPoints.add(point);
            }
        }

        return new AdminDashboardHistoryResponse(normalizedRangeMinutes, filteredPoints);
    }

    private int normalizeRangeMinutes(Integer rangeMinutes) {
        if (rangeMinutes == null || rangeMinutes <= 0) {
            return DEFAULT_RANGE_MINUTES;
        }
        return Math.min(rangeMinutes, MAX_RANGE_MINUTES);
    }

    private void evictOldPoints(OffsetDateTime now) {
        OffsetDateTime cutoff = now.minusMinutes(MAX_RANGE_MINUTES);
        while (!points.isEmpty()) {
            AdminMetricsHistoryPoint oldest = points.peekFirst();
            if (oldest == null || !oldest.timestamp().isBefore(cutoff)) {
                return;
            }
            points.pollFirst();
        }
    }

    private Double percent(Long used, Long total) {
        if (used == null || total == null || total <= 0) {
            return null;
        }
        return ((double) used / total) * 100;
    }

    private Double failureRate(TrafficMetricsResponse traffic, AiUsageSummary ai) {
        long requestFailures = safe(traffic.clientErrorCount5m()) + safe(traffic.serverErrorCount5m());
        long aiFailures = safe(ai.getFailureCountToday());
        long totalFailures = requestFailures + aiFailures;
        long totalRequests = safe(traffic.requestCount5m()) + safe(ai.getRequestCountToday());

        if (totalRequests == 0) {
            return 0.0;
        }

        return ((double) totalFailures / totalRequests) * 100;
    }

    private long safe(Long value) {
        return value == null ? 0L : value;
    }

    private long totalTokenDelta(long currentTotalTokens5m) {
        AdminMetricsHistoryPoint previous = points.peekLast();
        if (previous == null || previous.totalTokens5m() == null) {
            return currentTotalTokens5m;
        }
        return Math.max(0L, currentTotalTokens5m - previous.totalTokens5m());
    }
}
