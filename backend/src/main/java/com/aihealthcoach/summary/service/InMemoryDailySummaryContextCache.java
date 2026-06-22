package com.aihealthcoach.summary.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextCacheEntry;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextVersion;

@Service
@ConditionalOnProperty(
        name = "ai.chat.summary.context-cache.type",
        havingValue = "memory",
        matchIfMissing = true
)
public class InMemoryDailySummaryContextCache implements DailySummaryContextCache {

    private final Map<CacheKey, CacheBundle> cache = new ConcurrentHashMap<>();
    private final Clock clock;
    private final Duration ttl;

    public InMemoryDailySummaryContextCache() {
        this(Clock.systemDefaultZone(), Duration.ofMinutes(5));
    }

    @Autowired
    public InMemoryDailySummaryContextCache(
            Clock clock,
            @Value("${ai.chat.summary.context-cache.ttl-ms:300000}") long ttlMillis
    ) {
        this(clock, Duration.ofMillis(ttlMillis));
    }

    InMemoryDailySummaryContextCache(Clock clock, Duration ttl) {
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public List<DailyChatSummaryContextResponse> getOrLoad(
            Long userId,
            LocalDate from,
            LocalDate to,
            Supplier<List<DailyChatSummaryContextResponse>> loader
    ) {
        CacheKey key = new CacheKey(userId, from, to);
        CacheBundle cached = cache.get(key);
        if (cached != null && !cached.isExpired(now())) {
            return cached.summaries();
        }

        List<DailyChatSummaryContextResponse> loadedSummaries = loader.get();
        cache.put(key, CacheBundle.withoutVersionMarker(loadedSummaries, expiresAt()));
        return List.copyOf(loadedSummaries);
    }

    @Override
    public List<DailyChatSummaryContextResponse> getOrLoad(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<DailyChatSummaryContextVersion> currentVersions,
            Supplier<List<DailyChatSummaryContextCacheEntry>> loader
    ) {
        CacheKey key = new CacheKey(userId, from, to);
        Map<LocalDate, Long> expectedVersions = toVersionMap(currentVersions);
        CacheBundle cached = cache.get(key);

        if (cached != null && cached.sourceVersions().equals(expectedVersions) && !cached.isExpired(now())) {
            return cached.summaries();
        }

        List<DailyChatSummaryContextCacheEntry> loadedEntries = loader.get();
        List<DailyChatSummaryContextCacheEntry> freshEntries = filterMatchingVersions(loadedEntries, expectedVersions);
        List<DailyChatSummaryContextResponse> summaries = toResponses(freshEntries);
        Map<LocalDate, Long> loadedVersions = toEntryVersionMap(freshEntries);
        CacheBundle bundle = new CacheBundle(summaries, loadedVersions, expiresAt());

        if (loadedVersions.equals(expectedVersions)) {
            cache.put(key, bundle);
        } else {
            cache.remove(key);
        }

        return summaries;
    }

    @Override
    public void evictUser(Long userId) {
        cache.keySet().removeIf(key -> key.userId().equals(userId));
    }

    @Override
    public void clear() {
        cache.clear();
    }

    private Map<LocalDate, Long> toVersionMap(List<DailyChatSummaryContextVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return Map.of();
        }
        return versions.stream()
                .sorted(Comparator.comparing(DailyChatSummaryContextVersion::summaryDate))
                .collect(Collectors.toMap(
                        DailyChatSummaryContextVersion::summaryDate,
                        DailyChatSummaryContextVersion::sourceVersion,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private List<DailyChatSummaryContextCacheEntry> filterMatchingVersions(
            List<DailyChatSummaryContextCacheEntry> entries,
            Map<LocalDate, Long> expectedVersions
    ) {
        if (entries == null || entries.isEmpty() || expectedVersions.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .filter(entry -> expectedVersions.containsKey(entry.summaryDate()))
                .filter(entry -> expectedVersions.get(entry.summaryDate()).equals(entry.sourceVersion()))
                .sorted(Comparator.comparing(DailyChatSummaryContextCacheEntry::summaryDate))
                .toList();
    }

    private List<DailyChatSummaryContextResponse> toResponses(List<DailyChatSummaryContextCacheEntry> entries) {
        return entries.stream()
                .map(entry -> new DailyChatSummaryContextResponse(entry.summaryDate(), entry.content()))
                .toList();
    }

    private Map<LocalDate, Long> toEntryVersionMap(List<DailyChatSummaryContextCacheEntry> entries) {
        if (entries.isEmpty()) {
            return Map.of();
        }
        return entries.stream()
                .collect(Collectors.toMap(
                        DailyChatSummaryContextCacheEntry::summaryDate,
                        DailyChatSummaryContextCacheEntry::sourceVersion,
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private LocalDateTime expiresAt() {
        return now().plus(ttl);
    }

    private record CacheKey(Long userId, LocalDate from, LocalDate to) {
    }

    private record CacheBundle(
            List<DailyChatSummaryContextResponse> summaries,
            Map<LocalDate, Long> sourceVersions,
            LocalDateTime expiresAt
    ) {
        private CacheBundle {
            summaries = List.copyOf(summaries);
            sourceVersions = Map.copyOf(sourceVersions);
        }

        private static CacheBundle withoutVersionMarker(
                List<DailyChatSummaryContextResponse> summaries,
                LocalDateTime expiresAt
        ) {
            return new CacheBundle(summaries, Map.of(), expiresAt);
        }

        private CacheBundle withExpiresAt(LocalDateTime expiresAt) {
            return new CacheBundle(summaries, sourceVersions, expiresAt);
        }

        private boolean isExpired(LocalDateTime now) {
            return !expiresAt.isAfter(now);
        }
    }
}
