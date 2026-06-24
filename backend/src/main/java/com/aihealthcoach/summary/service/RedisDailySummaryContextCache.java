package com.aihealthcoach.summary.service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextCacheEntry;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextResponse;
import com.aihealthcoach.summary.dto.DailyChatSummaryDto.DailyChatSummaryContextVersion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@ConditionalOnProperty(name = "ai.chat.summary.context-cache.type", havingValue = "redis")
public class RedisDailySummaryContextCache implements DailySummaryContextCache {

    private static final String KEY_PREFIX = "ai:chat:summary-context";
    private static final String USER_KEYS_PREFIX = "ai:chat:summary-context-keys";
    private static final String TTL_POLICY_NEXT_MIDNIGHT = "next-midnight";
    private static final String TTL_POLICY_FIXED = "fixed";
    private static final Duration MINIMUM_TTL = Duration.ofSeconds(1);
    private static final TypeReference<CachePayload> CACHE_PAYLOAD_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final String ttlPolicy;
    private final Duration fixedTtl;

    @Autowired
    public RedisDailySummaryContextCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            @Value("${ai.chat.summary.context-cache.ttl-policy:next-midnight}") String ttlPolicy,
            @Value("${ai.chat.summary.context-cache.ttl-ms:300000}") long ttlMillis
    ) {
        this(redisTemplate, objectMapper, clock, ttlPolicy, Duration.ofMillis(ttlMillis));
    }

    RedisDailySummaryContextCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            Clock clock,
            String ttlPolicy,
            Duration fixedTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.ttlPolicy = ttlPolicy;
        this.fixedTtl = fixedTtl;
    }

    @Override
    public List<DailyChatSummaryContextResponse> getOrLoad(
            Long userId,
            LocalDate from,
            LocalDate to,
            Supplier<List<DailyChatSummaryContextResponse>> loader
    ) {
        String key = cacheKey(userId, from, to);
        CachePayload cached = readPayload(key, userId);
        if (cached != null && cached.hasNoVersionMarker()) {
            return cached.summaries();
        }

        List<DailyChatSummaryContextResponse> loadedSummaries = List.copyOf(loader.get());
        writePayload(key, userId, CachePayload.withoutVersionMarker(loadedSummaries));
        return loadedSummaries;
    }

    @Override
    public List<DailyChatSummaryContextResponse> getOrLoad(
            Long userId,
            LocalDate from,
            LocalDate to,
            List<DailyChatSummaryContextVersion> currentVersions,
            Supplier<List<DailyChatSummaryContextCacheEntry>> loader
    ) {
        String key = cacheKey(userId, from, to);
        Map<LocalDate, Long> expectedVersions = toVersionMap(currentVersions);
        CachePayload cached = readPayload(key, userId);
        if (cached != null && cached.sourceVersions().equals(expectedVersions)) {
            return cached.summaries();
        }

        List<DailyChatSummaryContextCacheEntry> loadedEntries = loader.get();
        List<DailyChatSummaryContextCacheEntry> freshEntries = filterMatchingVersions(loadedEntries, expectedVersions);
        List<DailyChatSummaryContextResponse> summaries = toResponses(freshEntries);
        Map<LocalDate, Long> loadedVersions = toEntryVersionMap(freshEntries);

        if (loadedVersions.equals(expectedVersions)) {
            writePayload(key, userId, new CachePayload(summaries, loadedVersions));
        } else {
            deleteKey(key, userId);
        }

        return summaries;
    }

    @Override
    public void evictUser(Long userId) {
        String userKeysKey = userKeysKey(userId);
        try {
            Set<String> keys = redisTemplate.opsForSet().members(userKeysKey);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
            redisTemplate.delete(userKeysKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to evict Redis daily summary context cache. user_id={}", userId, exception);
        }
    }

    @Override
    public void clear() {
        log.warn("Redis daily summary context cache clear requested, but wildcard keyspace scan is disabled");
    }

    private CachePayload readPayload(String key, Long userId) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, CACHE_PAYLOAD_TYPE);
        } catch (JsonProcessingException exception) {
            log.warn("Failed to deserialize Redis daily summary context cache. user_id={}", userId, exception);
            deleteKey(key, userId);
            return null;
        } catch (RuntimeException exception) {
            log.warn("Failed to read Redis daily summary context cache. user_id={}", userId, exception);
            return null;
        }
    }

    private void writePayload(String key, Long userId, CachePayload payload) {
        Duration ttl = ttl();
        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForValue().set(key, json, ttl);
            redisTemplate.opsForSet().add(userKeysKey(userId), key);
            redisTemplate.expire(userKeysKey(userId), ttl.plus(Duration.ofDays(1)));
        } catch (JsonProcessingException exception) {
            log.warn("Failed to serialize Redis daily summary context cache. user_id={}", userId, exception);
        } catch (RuntimeException exception) {
            log.warn("Failed to write Redis daily summary context cache. user_id={}", userId, exception);
        }
    }

    private void deleteKey(String key, Long userId) {
        try {
            redisTemplate.delete(key);
            redisTemplate.opsForSet().remove(userKeysKey(userId), key);
        } catch (RuntimeException exception) {
            log.warn("Failed to delete Redis daily summary context cache key. user_id={}", userId, exception);
        }
    }

    private Duration ttl() {
        if (TTL_POLICY_FIXED.equalsIgnoreCase(ttlPolicy)) {
            return fixedTtl.compareTo(MINIMUM_TTL) < 0 ? MINIMUM_TTL : fixedTtl;
        }
        if (!TTL_POLICY_NEXT_MIDNIGHT.equalsIgnoreCase(ttlPolicy)) {
            log.warn("Unknown daily summary context cache TTL policy. policy={}", ttlPolicy);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime nextMidnight = LocalDate.now(clock).plusDays(1).atStartOfDay();
        Duration ttl = Duration.between(now, nextMidnight);
        return ttl.compareTo(MINIMUM_TTL) < 0 ? MINIMUM_TTL : ttl;
    }

    private String cacheKey(Long userId, LocalDate from, LocalDate to) {
        return KEY_PREFIX + ":" + userId + ":" + from + ":" + to;
    }

    private String userKeysKey(Long userId) {
        return USER_KEYS_PREFIX + ":" + userId;
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

    private record CachePayload(
            List<DailyChatSummaryContextResponse> summaries,
            Map<LocalDate, Long> sourceVersions
    ) {
        private CachePayload {
            summaries = summaries == null ? List.of() : List.copyOf(summaries);
            sourceVersions = sourceVersions == null ? Map.of() : Map.copyOf(sourceVersions);
        }

        private static CachePayload withoutVersionMarker(List<DailyChatSummaryContextResponse> summaries) {
            return new CachePayload(summaries, Map.of());
        }

        private boolean hasNoVersionMarker() {
            return sourceVersions.isEmpty();
        }
    }
}
