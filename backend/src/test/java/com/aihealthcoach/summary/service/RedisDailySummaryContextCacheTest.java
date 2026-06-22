package com.aihealthcoach.summary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisDailySummaryContextCacheTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 6, 16);
    private static final LocalDate TO = LocalDate.of(2026, 6, 21);
    private static final String CACHE_KEY = "ai:chat:summary-context:1:2026-06-16:2026-06-21";
    private static final String USER_KEYS_KEY = "ai:chat:summary-context-keys:1";
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-22T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    private ObjectMapper objectMapper;
    private RedisDailySummaryContextCache cache;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        cache = new RedisDailySummaryContextCache(
                redisTemplate,
                objectMapper,
                CLOCK,
                "next-midnight",
                Duration.ofMinutes(5)
        );
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    void cacheHitReturnsRedisValueWithoutCallingLoader() throws Exception {
        String cachedJson = objectMapper.writeValueAsString(new CachePayloadForTest(List.of(
                response(LocalDate.of(2026, 6, 21), "redis summary")
        )));
        when(valueOperations.get(CACHE_KEY)).thenReturn(cachedJson);
        AtomicInteger loadCount = new AtomicInteger();

        List<DailyChatSummaryContextResponse> summaries = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> {
                    loadCount.incrementAndGet();
                    return List.of(response(LocalDate.of(2026, 6, 21), "db summary"));
                }
        );

        assertThat(loadCount).hasValue(0);
        assertThat(summaries).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("redis summary");
        verify(valueOperations, never()).set(any(), any(), any(Duration.class));
    }

    @Test
    void cacheMissCallsLoaderAndWritesRedisWithNextMidnightTtl() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);

        List<DailyChatSummaryContextResponse> summaries = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> List.of(response(LocalDate.of(2026, 6, 21), "db summary"))
        );

        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        assertThat(summaries).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("db summary");
        verify(valueOperations).set(eq(CACHE_KEY), any(String.class), ttlCaptor.capture());
        verify(setOperations).add(USER_KEYS_KEY, CACHE_KEY);
        verify(redisTemplate).expire(USER_KEYS_KEY, Duration.ofHours(36));
        assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(12));
    }

    @Test
    void fixedTtlPolicyUsesConfiguredDuration() {
        cache = new RedisDailySummaryContextCache(
                redisTemplate,
                objectMapper,
                CLOCK,
                "fixed",
                Duration.ofMinutes(5)
        );
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);

        cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> List.of(response(LocalDate.of(2026, 6, 21), "db summary"))
        );

        verify(valueOperations).set(eq(CACHE_KEY), any(String.class), eq(Duration.ofMinutes(5)));
    }

    @Test
    void redisGetFailureFallsBackToLoader() {
        when(valueOperations.get(CACHE_KEY)).thenThrow(new IllegalStateException("redis down"));

        List<DailyChatSummaryContextResponse> summaries = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> List.of(response(LocalDate.of(2026, 6, 21), "fallback summary"))
        );

        assertThat(summaries).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("fallback summary");
        verify(valueOperations).set(eq(CACHE_KEY), any(String.class), any(Duration.class));
    }

    @Test
    void redisWriteFailureReturnsLoaderResult() {
        when(valueOperations.get(CACHE_KEY)).thenReturn(null);
        doThrow(new IllegalStateException("write failed"))
                .when(valueOperations).set(eq(CACHE_KEY), any(String.class), any(Duration.class));

        List<DailyChatSummaryContextResponse> summaries = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> List.of(response(LocalDate.of(2026, 6, 21), "fallback summary"))
        );

        assertThat(summaries).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("fallback summary");
    }

    @Test
    void invalidJsonFallsBackToLoaderAndRewritesCache() {
        when(valueOperations.get(CACHE_KEY)).thenReturn("{not-json");

        List<DailyChatSummaryContextResponse> summaries = cache.getOrLoad(
                USER_ID,
                FROM,
                TO,
                () -> List.of(response(LocalDate.of(2026, 6, 21), "fresh summary"))
        );

        assertThat(summaries).extracting(DailyChatSummaryContextResponse::content)
                .containsExactly("fresh summary");
        verify(redisTemplate).delete(CACHE_KEY);
        verify(setOperations).remove(USER_KEYS_KEY, CACHE_KEY);
        verify(valueOperations).set(eq(CACHE_KEY), any(String.class), any(Duration.class));
    }

    @Test
    void evictUserDeletesTrackedKeysWithoutWildcardScan() {
        when(setOperations.members(USER_KEYS_KEY)).thenReturn(Set.of(CACHE_KEY));

        cache.evictUser(USER_ID);

        verify(redisTemplate).delete(Set.of(CACHE_KEY));
        verify(redisTemplate).delete(USER_KEYS_KEY);
    }

    private DailyChatSummaryContextResponse response(LocalDate date, String content) {
        return new DailyChatSummaryContextResponse(date, content);
    }

    private record CachePayloadForTest(
            List<DailyChatSummaryContextResponse> summaries,
            java.util.Map<LocalDate, Long> sourceVersions
    ) {
        private CachePayloadForTest(List<DailyChatSummaryContextResponse> summaries) {
            this(summaries, java.util.Map.of());
        }
    }
}
