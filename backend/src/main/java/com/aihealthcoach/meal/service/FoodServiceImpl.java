package com.aihealthcoach.meal.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodGroupResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodSearchPageResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodServingResponse;
import com.aihealthcoach.meal.entity.Food;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.meal.util.FoodSearchQuery;
import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;

    private final MealMapper mealMapper;

    @Override
    @Transactional(readOnly = true)
    public FoodSearchPageResponse searchFoodGroups(String query, int page, int size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;

        String trimmedQuery = FoodSearchQuery.normalize(query);
        List<Token> tokens = FoodSearchQuery.tokens(trimmedQuery);

        long totalItems = mealMapper.countFoodGroups(trimmedQuery, tokens);
        int totalPages = calculateTotalPages(totalItems, normalizedSize);
        List<String> sourceKeys = mealMapper.findFoodGroupSourceKeys(trimmedQuery, tokens, normalizedSize, offset);

        if (sourceKeys.isEmpty()) {
            return new FoodSearchPageResponse(List.of(), normalizedPage, normalizedSize, totalItems, totalPages);
        }

        List<Food> rows = mealMapper.findFoodsBySourceKeys(sourceKeys);
        return new FoodSearchPageResponse(
                toGroupResponses(sourceKeys, rows),
                normalizedPage,
                normalizedSize,
                totalItems,
                totalPages
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoodCandidateResponse> searchFoods(String query) {
        String trimmedQuery = FoodSearchQuery.normalize(query);
        List<Token> tokens = FoodSearchQuery.tokens(trimmedQuery);

        if (tokens.isEmpty()) {
            return List.of();
        }

        return mealMapper.searchFoods(trimmedQuery, tokens).stream()
                .map(FoodCandidateResponse::fromRow)
                .toList();
    }

    private List<FoodGroupResponse> toGroupResponses(List<String> sourceKeys, List<Food> rows) {
        Map<String, List<Food>> rowsBySourceKey = new LinkedHashMap<>();

        for (String sourceKey : sourceKeys) {
            rowsBySourceKey.put(sourceKey, List.of());
        }

        for (Food row : rows) {
            rowsBySourceKey.compute(row.getSourceKey(), (sourceKey, existingRows) -> {
                if (existingRows == null || existingRows.isEmpty()) {
                    return new java.util.ArrayList<>(List.of(row));
                }

                existingRows.add(row);
                return existingRows;
            });
        }

        return rowsBySourceKey.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> toGroupResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private FoodGroupResponse toGroupResponse(String sourceKey, List<Food> rows) {
        Food representative = rows.get(0);
        return new FoodGroupResponse(
                sourceKey,
                representative.getFoodName(),
                representative.getBrand(),
                rows.stream()
                        .map(FoodServingResponse::fromRow)
                        .toList()
        );
    }

    private int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        return size < 1 ? DEFAULT_SIZE : size;
    }

    private int calculateTotalPages(long totalItems, int size) {
        if (totalItems == 0) {
            return 0;
        }

        return (int) Math.ceil((double) totalItems / size);
    }
}
