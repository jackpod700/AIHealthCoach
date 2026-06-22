package com.aihealthcoach.summary.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextCacheEntry;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextVersion;

class InMemoryDailySummaryContextCacheTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final LocalDate FROM = LocalDate.of(2026, 6, 16);
    private static final LocalDate TO = LocalDate.of(2026, 6, 21);

    private InMemoryDailySummaryContextCache cache;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-06-22T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        cache = new InMemoryDailySummaryContextCache(clock, Duration.ofMinutes(5));
    }

    @Test
    void getOrLoadWithoutVersionMarkerReturnsCachedSummariesWithinTtl() {
        AtomicInteger loadCount = new AtomicInteger();

        List<DailyChatSummaryContextResponse> first = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(response(LocalDate.of(2026, 6, 21), "cached summary"));
                }
        );
        clock.plus(Duration.ofMinutes(4));
        List<DailyChatSummaryContextResponse> second = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(response(LocalDate.of(2026, 6, 21), "reloaded summary"));
                }
        );

        assertThat(loadCount).hasValue(1);
        assertThat(second).isEqualTo(first);
        assertThat(second).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("cached summary");
    }

    @Test
    void getOrLoadWithoutVersionMarkerReloadsAfterTtlExpires() {
        AtomicInteger loadCount = new AtomicInteger();

        cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(response(LocalDate.of(2026, 6, 21), "cached summary"));
                }
        );
        clock.plus(Duration.ofMinutes(5));

        List<DailyChatSummaryContextResponse> reloaded = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(response(LocalDate.of(2026, 6, 21), "reloaded summary"));
                }
        );

        assertThat(loadCount).hasValue(2);
        assertThat(reloaded).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("reloaded summary");
    }

    @Test
    void evictUserReloadsWithoutWaitingForTtl() {
        AtomicInteger loadCount = new AtomicInteger();

        cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(response(LocalDate.of(2026, 6, 21), "cached summary"));
                }
        );

        cache.evictUser(USER_ID);

        List<DailyChatSummaryContextResponse> reloaded = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(response(LocalDate.of(2026, 6, 21), "reloaded after evict"));
                }
        );

        assertThat(loadCount).hasValue(2);
        assertThat(reloaded).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("reloaded after evict");
    }

    @Test
    void getOrLoadReturnsCachedSummariesWhenVersionMarkerMatches() {
        AtomicInteger loadCount = new AtomicInteger();
        List<DailyChatSummaryContextVersion> versions = List.of(
                version(LocalDate.of(2026, 6, 20), 2L),
                version(LocalDate.of(2026, 6, 21), 3L)
        );

        List<DailyChatSummaryContextResponse> first = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                versions,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(
                            entry(LocalDate.of(2026, 6, 20), "첫 번째 요약", 2L),
                            entry(LocalDate.of(2026, 6, 21), "두 번째 요약", 3L)
                    );
                }
        );
        List<DailyChatSummaryContextResponse> second = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                versions,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(entry(LocalDate.of(2026, 6, 21), "다시 로드되면 안 됨", 3L));
                }
        );

        assertThat(loadCount).hasValue(1);
        assertThat(first).isEqualTo(second);
        assertThat(second).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("첫 번째 요약", "두 번째 요약");
    }

    @Test
    void getOrLoadReloadsWhenVersionMarkerChanges() {
        AtomicInteger loadCount = new AtomicInteger();

        cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                List.of(version(LocalDate.of(2026, 6, 21), 3L)),
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(entry(LocalDate.of(2026, 6, 21), "이전 요약", 3L));
                }
        );

        List<DailyChatSummaryContextResponse> reloaded = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                List.of(version(LocalDate.of(2026, 6, 21), 4L)),
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(entry(LocalDate.of(2026, 6, 21), "새 요약", 4L));
                }
        );

        assertThat(loadCount).hasValue(2);
        assertThat(reloaded).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("새 요약");
    }

    @Test
    void getOrLoadExcludesEntriesThatDoNotMatchFreshVersionMarker() {
        List<DailyChatSummaryContextResponse> summaries = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                List.of(version(LocalDate.of(2026, 6, 21), 4L)),
                () -> List.of(
                        entry(LocalDate.of(2026, 6, 20), "stale 상태라 marker에 없음", 2L),
                        entry(LocalDate.of(2026, 6, 21), "version mismatch", 3L),
                        entry(LocalDate.of(2026, 6, 21), "fresh version", 4L)
                )
        );

        assertThat(summaries).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("fresh version");
    }

    @Test
    void evictUserRemovesOnlyThatUsersCachedBundles() {
        cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                List.of(version(LocalDate.of(2026, 6, 21), 1L)),
                () -> List.of(entry(LocalDate.of(2026, 6, 21), "user summary", 1L))
        );
        cache.getOrLoad(
                OTHER_USER_ID,
                FROM,
                TO,
                List.of(version(LocalDate.of(2026, 6, 21), 1L)),
                () -> List.of(entry(LocalDate.of(2026, 6, 21), "other user summary", 1L))
        );

        cache.evictUser(USER_ID);

        AtomicInteger userLoadCount = new AtomicInteger();
        AtomicInteger otherUserLoadCount = new AtomicInteger();
        cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                List.of(version(LocalDate.of(2026, 6, 21), 1L)),
                () -> {
                    userLoadCount.incrementAndGet();
                    return List.of(entry(LocalDate.of(2026, 6, 21), "user reloaded", 1L));
                }
        );
        List<DailyChatSummaryContextResponse> otherUserSummaries = cache.getOrLoad(
                OTHER_USER_ID,
                FROM,
                TO,
                List.of(version(LocalDate.of(2026, 6, 21), 1L)),
                () -> {
                    otherUserLoadCount.incrementAndGet();
                    return List.of(entry(LocalDate.of(2026, 6, 21), "other reloaded", 1L));
                }
        );

        assertThat(userLoadCount).hasValue(1);
        assertThat(otherUserLoadCount).hasValue(0);
        assertThat(otherUserSummaries).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("other user summary");
    }

    private DailyChatSummaryContextCacheEntry entry(LocalDate summaryDate, String content, Long sourceVersion) {
        return new DailyChatSummaryContextCacheEntry(summaryDate, content, sourceVersion);
    }

    private DailyChatSummaryContextResponse response(LocalDate summaryDate, String content) {
        return new DailyChatSummaryContextResponse(summaryDate, content);
    }

    private DailyChatSummaryContextVersion version(LocalDate summaryDate, Long sourceVersion) {
        return new DailyChatSummaryContextVersion(summaryDate, sourceVersion);
    }

    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void plus(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
