package com.aihealthcoach.admin.service;

import java.time.Clock;
import java.time.Instant;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ActiveUserMetricsService {

    private static final long ACTIVE_WINDOW_SECONDS = 5 * 60;

    private final Clock clock;
    private final Deque<ActiveUserMetric> activeUserMetrics = new ConcurrentLinkedDeque<>();

    public void record(Long userId) {
        if (userId == null) {
            return;
        }

        Instant now = Instant.now(clock);
        activeUserMetrics.addLast(new ActiveUserMetric(userId, now));
        evictOldMetrics(now);
    }

    public long activeUsers5m() {
        Instant now = Instant.now(clock);
        evictOldMetrics(now);

        Set<Long> activeUserIds = new HashSet<>();
        for (ActiveUserMetric metric : activeUserMetrics) {
            if (ageSeconds(now, metric.createdAt()) <= ACTIVE_WINDOW_SECONDS) {
                activeUserIds.add(metric.userId());
            }
        }
        return activeUserIds.size();
    }

    private void evictOldMetrics(Instant now) {
        while (!activeUserMetrics.isEmpty()) {
            ActiveUserMetric oldest = activeUserMetrics.peekFirst();
            if (oldest == null || ageSeconds(now, oldest.createdAt()) <= ACTIVE_WINDOW_SECONDS) {
                return;
            }
            activeUserMetrics.pollFirst();
        }
    }

    private long ageSeconds(Instant now, Instant createdAt) {
        return now.getEpochSecond() - createdAt.getEpochSecond();
    }

    private record ActiveUserMetric(Long userId, Instant createdAt) {
    }
}
