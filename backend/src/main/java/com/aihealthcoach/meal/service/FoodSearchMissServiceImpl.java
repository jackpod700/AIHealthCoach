package com.aihealthcoach.meal.service;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissCreateRequest;
import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissResponse;
import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.FoodImportMapper;
import com.aihealthcoach.meal.util.FoodSearchQuery;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodSearchMissServiceImpl implements FoodSearchMissService {

    private static final int MIN_COMPACT_QUERY_LENGTH = 2;
    private static final int MAX_QUERY_LENGTH = 100;
    private static final int MAX_FAILURE_REASON_LENGTH = 1000;
    private static final Pattern MEANINGFUL_CHARACTER = Pattern.compile(".*[\\p{L}\\p{N}].*");

    private final FoodImportMapper foodImportMapper;

    @Override
    @Transactional
    public FoodSearchMissResponse recordMiss(Long userId, FoodSearchMissCreateRequest request) {
        String query = normalizeRequestQuery(request);
        String normalizedQuery = FoodSearchQuery.normalize(query);

        if (shouldIgnore(normalizedQuery)) {
            return new FoodSearchMissResponse(false, "IGNORED", null, query, normalizedQuery, null);
        }

        Long searchMissId = foodImportMapper.upsertFoodSearchMiss(query, normalizedQuery, userId);
        FoodSearchMiss saved = foodImportMapper.findFoodSearchMissById(searchMissId);
        return new FoodSearchMissResponse(
                true,
                saved.getStatus(),
                saved.getId(),
                saved.getQuery(),
                saved.getNormalizedQuery(),
                saved.getMissCount()
        );
    }

    @Override
    @Transactional
    public List<FoodSearchMiss> pickPendingMisses(int limit, int maxRetries) {
        if (limit < 1) {
            return List.of();
        }

        List<FoodSearchMiss> misses = foodImportMapper.pickPendingFoodSearchMisses(limit, maxRetries);
        for (FoodSearchMiss miss : misses) {
            foodImportMapper.markFoodSearchMissProcessing(miss.getId());
        }
        return misses;
    }

    @Override
    @Transactional
    public void markPendingReview(Long searchMissId) {
        foodImportMapper.markFoodSearchMissPendingReview(searchMissId);
    }

    @Override
    @Transactional
    public void markNoResult(Long searchMissId) {
        foodImportMapper.markFoodSearchMissNoResult(searchMissId);
    }

    @Override
    @Transactional
    public void markFailed(Long searchMissId, String failureReason) {
        foodImportMapper.markFoodSearchMissFailed(searchMissId, truncate(failureReason));
    }

    private String normalizeRequestQuery(FoodSearchMissCreateRequest request) {
        if (request == null) {
            throw MealException.invalidFoodSearchMiss();
        }

        String query = FoodSearchQuery.normalize(request.query());
        if (query.isBlank()) {
            throw MealException.invalidFoodSearchMiss();
        }
        if (query.length() > MAX_QUERY_LENGTH) {
            query = query.substring(0, MAX_QUERY_LENGTH).trim();
        }
        return query;
    }

    private boolean shouldIgnore(String normalizedQuery) {
        String compactQuery = FoodSearchQuery.compact(normalizedQuery);
        return compactQuery.length() < MIN_COMPACT_QUERY_LENGTH
                || !MEANINGFUL_CHARACTER.matcher(normalizedQuery).matches();
    }

    private String truncate(String value) {
        String normalized = Objects.toString(value, "Unknown failure.").trim();
        if (normalized.length() <= MAX_FAILURE_REASON_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
