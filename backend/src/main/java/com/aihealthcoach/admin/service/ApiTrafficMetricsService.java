package com.aihealthcoach.admin.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;

import com.aihealthcoach.admin.dto.AdminDto.TrafficMetricsResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ApiTrafficMetricsService {

    private static final long ONE_MINUTE_SECONDS = 60;
    private static final long FIVE_MINUTES_SECONDS = 5 * 60;
    private static final long ONE_HOUR_SECONDS = 60 * 60;

    private final Clock clock;
    private final Deque<ApiRequestMetric> metrics = new ConcurrentLinkedDeque<>();

    public void record(String path, int status, long durationMs, Long userId) {
        Instant now = Instant.now(clock);
        metrics.addLast(new ApiRequestMetric(path, status, durationMs, userId, now));
        evictOldMetrics(now);
    }

    public TrafficMetricsResponse snapshot() {
        Instant now = Instant.now(clock);
        evictOldMetrics(now);

        long requestCount1m = 0;
        long requestCount5m = 0;
        long requestCount1h = 0;
        long clientErrorCount5m = 0;
        long serverErrorCount5m = 0;
        long responseTimeSum5m = 0;

        for (ApiRequestMetric metric : metrics) {
            long ageSeconds = now.getEpochSecond() - metric.createdAt().getEpochSecond();

            if (ageSeconds <= ONE_HOUR_SECONDS) {
                requestCount1h++;
            }
            if (ageSeconds <= FIVE_MINUTES_SECONDS) {
                requestCount5m++;
                responseTimeSum5m += metric.durationMs();
                if (metric.status() >= 400 && metric.status() < 500) {
                    clientErrorCount5m++;
                }
                if (metric.status() >= 500) {
                    serverErrorCount5m++;
                }
            }
            if (ageSeconds <= ONE_MINUTE_SECONDS) {
                requestCount1m++;
            }
        }

        double averageResponseMs5m = requestCount5m == 0 ? 0 : (double) responseTimeSum5m / requestCount5m;

        return new TrafficMetricsResponse(
                requestCount1m,
                requestCount5m,
                requestCount1h,
                averageResponseMs5m,
                clientErrorCount5m,
                serverErrorCount5m
        );
    }

    public long activeUsers5m() {
        Instant now = Instant.now(clock);
        evictOldMetrics(now);

        Set<Long> activeUserIds = new HashSet<>();
        for (ApiRequestMetric metric : metrics) {
            long ageSeconds = now.getEpochSecond() - metric.createdAt().getEpochSecond();
            if (ageSeconds <= FIVE_MINUTES_SECONDS && metric.userId() != null) {
                activeUserIds.add(metric.userId());
            }
        }
        return activeUserIds.size();
    }

    private void evictOldMetrics(Instant now) {
        while (!metrics.isEmpty()) {
            ApiRequestMetric oldest = metrics.peekFirst();
            if (oldest == null || now.getEpochSecond() - oldest.createdAt().getEpochSecond() <= ONE_HOUR_SECONDS) {
                return;
            }
            metrics.pollFirst();
        }
    }

    private record ApiRequestMetric(
            String path,
            int status,
            long durationMs,
            Long userId,
            Instant createdAt
    ) {
    }
}
