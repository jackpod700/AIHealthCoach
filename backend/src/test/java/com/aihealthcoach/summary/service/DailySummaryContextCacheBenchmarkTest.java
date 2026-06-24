package com.aihealthcoach.summary.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.summary.mapper.DailyChatSummaryMapper;

@Tag("benchmark")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_CACHE_BENCHMARK", matches = "true")
@SpringBootTest(properties = {
        "ai.chat.summary.context-cache.type=redis",
        "ai.chat.summary.context-cache.ttl-policy=fixed",
        "ai.chat.summary.context-cache.ttl-ms=300000",
        "spring.ai.openai.api-key=test"
})
class DailySummaryContextCacheBenchmarkTest {

    private static final Long USER_ID = 920500L;
    private static final LocalDate FROM = LocalDate.now().minusDays(6);
    private static final LocalDate TO = LocalDate.now().minusDays(1);
    private static final int DEFAULT_ITERATIONS = 100;
    private static final int DEFAULT_WARMUP_ITERATIONS = 10;
    private static final String CACHE_KEY = "ai:chat:summary-context:" + USER_ID + ":" + FROM + ":" + TO;
    private static final String USER_KEYS_KEY = "ai:chat:summary-context-keys:" + USER_ID;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private DailyChatSummaryMapper dailyChatSummaryMapper;
    @Autowired
    private DailySummaryContextCache dailySummaryContextCache;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private int iterations;
    private int warmupIterations;

    @BeforeAll
    void setUpBenchmarkData() throws Exception {
        iterations = intEnv("REDIS_CACHE_BENCHMARK_ITERATIONS", DEFAULT_ITERATIONS);
        warmupIterations = intEnv("REDIS_CACHE_BENCHMARK_WARMUP_ITERATIONS", DEFAULT_WARMUP_ITERATIONS);
        if (booleanEnv("REDIS_CACHE_BENCHMARK_SEED", true)) {
            seedBenchmarkData();
        }
        clearBenchmarkRedisKeys();

        List<DailyChatSummaryContextResponse> summaries = dailyChatSummaryMapper
                .findFreshSummariesBetween(USER_ID, FROM, TO);
        assertThat(summaries)
                .as("benchmark seed should create fresh summaries for user " + USER_ID)
                .isNotEmpty();
    }

    @Test
    void compareDbDirectRedisMissRedisHitAndEvictOnRealSpringPath() {
        Supplier<List<DailyChatSummaryContextResponse>> dbLoader =
                () -> dailyChatSummaryMapper.findFreshSummariesBetween(USER_ID, FROM, TO);

        for (int i = 0; i < warmupIterations; i++) {
            dailyChatSummaryMapper.findFreshSummariesBetween(USER_ID, FROM, TO);
            dailySummaryContextCache.evictUser(USER_ID);
            dailySummaryContextCache.getOrLoad(USER_ID, FROM, TO, dbLoader);
            dailySummaryContextCache.getOrLoad(USER_ID, FROM, TO, failIfCalledLoader());
        }

        BenchmarkResult dbDirect = measure("DB direct fresh summary lookup", iterations,
                () -> dailyChatSummaryMapper.findFreshSummariesBetween(USER_ID, FROM, TO));

        BenchmarkResult redisMiss = measure("Redis miss fallback DB lookup + cache write", iterations, () -> {
            dailySummaryContextCache.evictUser(USER_ID);
            return dailySummaryContextCache.getOrLoad(USER_ID, FROM, TO, dbLoader);
        });

        dailySummaryContextCache.evictUser(USER_ID);
        dailySummaryContextCache.getOrLoad(USER_ID, FROM, TO, dbLoader);
        AtomicInteger redisHitLoaderCount = new AtomicInteger();
        BenchmarkResult redisHit = measure("Redis cache hit getOrLoad", iterations,
                () -> dailySummaryContextCache.getOrLoad(USER_ID, FROM, TO, () -> {
                    redisHitLoaderCount.incrementAndGet();
                    throw new AssertionError("Redis hit benchmark should not call DB loader");
                }));

        BenchmarkResult redisEvict = measure("Redis evictUser", iterations, () -> {
            dailySummaryContextCache.getOrLoad(USER_ID, FROM, TO, dbLoader);
            dailySummaryContextCache.evictUser(USER_ID);
            return List.of();
        });

        assertThat(redisHitLoaderCount).hasValue(0);
        printResults(List.of(dbDirect, redisMiss, redisHit, redisEvict));
    }

    private BenchmarkResult measure(
            String label,
            int count,
            Supplier<List<DailyChatSummaryContextResponse>> action
    ) {
        List<Double> elapsedMillis = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            long start = System.nanoTime();
            List<DailyChatSummaryContextResponse> result = action.get();
            long end = System.nanoTime();
            assertThat(result).isNotNull();
            elapsedMillis.add((end - start) / 1_000_000.0);
        }
        return BenchmarkResult.from(label, elapsedMillis);
    }

    private Supplier<List<DailyChatSummaryContextResponse>> failIfCalledLoader() {
        return () -> {
            throw new AssertionError("loader should not be called");
        };
    }

    private void seedBenchmarkData() throws Exception {
        int changeRate = intEnv("REDIS_CACHE_BENCHMARK_CHANGE_RATE", 1);
        Path seedPath = Path.of("../data/db/benchmark/seed-daily-summary-context-cache-benchmark.sql");
        String sql = Files.readString(seedPath, StandardCharsets.UTF_8)
                .replace(":CHANGE_RATE", Integer.toString(changeRate));

        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(
                    connection,
                    new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8), seedPath.toString())
            );
        }
    }

    private void clearBenchmarkRedisKeys() {
        redisTemplate.delete(CACHE_KEY);
        redisTemplate.delete(USER_KEYS_KEY);
    }

    private int intEnv(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private boolean booleanEnv(String name, boolean defaultValue) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private void printResults(List<BenchmarkResult> results) {
        System.out.println();
        System.out.println("## Daily summary context Redis cache benchmark");
        System.out.printf(
                "settings userId=%d from=%s to=%s iterations=%d warmup=%d%n",
                USER_ID,
                FROM,
                TO,
                iterations,
                warmupIterations
        );
        System.out.println("| Path | Count | Min | Avg | P50 | P95 | P99 | Max |");
        System.out.println("|---|---:|---:|---:|---:|---:|---:|---:|");
        results.forEach(result -> System.out.printf(
                "| %s | %d | %.3fms | %.3fms | %.3fms | %.3fms | %.3fms | %.3fms |%n",
                result.label(),
                result.count(),
                result.min(),
                result.avg(),
                result.p50(),
                result.p95(),
                result.p99(),
                result.max()
        ));
        System.out.println();
    }

    private record BenchmarkResult(
            String label,
            int count,
            double min,
            double avg,
            double p50,
            double p95,
            double p99,
            double max
    ) {
        private static BenchmarkResult from(String label, List<Double> elapsedMillis) {
            List<Double> sorted = elapsedMillis.stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();
            double sum = elapsedMillis.stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
            return new BenchmarkResult(
                    label,
                    elapsedMillis.size(),
                    sorted.getFirst(),
                    sum / elapsedMillis.size(),
                    percentile(sorted, 0.50),
                    percentile(sorted, 0.95),
                    percentile(sorted, 0.99),
                    sorted.getLast()
            );
        }

        private static double percentile(List<Double> sorted, double percentile) {
            int index = (int) Math.ceil(percentile * sorted.size()) - 1;
            int boundedIndex = Math.max(0, Math.min(index, sorted.size() - 1));
            return sorted.get(boundedIndex);
        }
    }
}
