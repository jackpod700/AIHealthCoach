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

        /*
         * 음식 조회 화면 전용 검색 흐름.
         *
         * foods 테이블은 기준량(serving)마다 row가 따로 저장된다.
         * 예를 들어 같은 source_key를 가진 "닭가슴살"이 100g, 1인분, 1쪽 row로 나뉘어 있을 수 있다.
         * 화면에서는 이것을 음식 카드 1개와 기준 선택 버튼 여러 개로 보여줘야 하므로 row 단위로 바로 page를 자르면 안 된다.
         *
         * 그래서 먼저 source_key 기준으로 "음식 그룹"을 page/size만큼 고르고,
         * 그 다음 선택된 source_key들에 속한 모든 serving row를 다시 조회한다.
         * 마지막으로 Java에서 source_key별 servings 배열로 묶어 화면용 응답을 만든다.
         */
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

        /*
         * 식단에 저장할 수 있는 음식 후보를 찾는다.
         * foods 테이블은 기준량마다 foodId가 다르므로, 후보 하나는 "음식명 + 기준량"이 확정된 row 하나를 의미한다.
         * 사용자는 이 후보의 foodId를 선택하고, 선택된 foodId가 meal_items.food_id로 저장된다.
         */
        return mealMapper.searchFoods(trimmedQuery, tokens).stream()
                .map(FoodCandidateResponse::fromRow)
                .toList();
    }

    private List<FoodGroupResponse> toGroupResponses(List<String> sourceKeys, List<Food> rows) {
        Map<String, List<Food>> rowsBySourceKey = new LinkedHashMap<>();

        /*
         * LinkedHashMap을 쓰는 이유는 응답 순서를 지키기 위해서다.
         * sourceKeys는 DB에서 검색 우선순위와 페이지 조건을 적용해 가져온 순서다.
         * HashMap을 쓰면 iteration 순서가 섞일 수 있으므로, 먼저 sourceKeys 순서대로 빈 그룹을 만들어 둔다.
         */
        for (String sourceKey : sourceKeys) {
            rowsBySourceKey.put(sourceKey, List.of());
        }

        /*
         * findFoodsBySourceKeys() 결과는 serving row들의 평평한 목록이다.
         * 각 row를 source_key에 맞는 그룹에 추가해서
         * FoodGroupResponse.servings 배열로 변환할 준비를 한다.
         */
        for (Food row : rows) {
            rowsBySourceKey.compute(row.getSourceKey(), (sourceKey, existingRows) -> {
                /*
                 * 위에서 빈 그룹은 List.of()로 만들어져 있어 immutable이다.
                 * 첫 row가 들어오는 시점에 mutable ArrayList로 교체하고,
                 * 이후 같은 source_key row들은 그 리스트에 계속 추가한다.
                 */
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
        /*
         * 같은 source_key에 속한 row들은 같은 음식의 서로 다른 기준량으로 본다.
         * 음식명과 제조사명은 같은 그룹 안에서 동일하다는 전제이므로 첫 row를 대표 정보로 사용한다.
         * 실제 식단 저장에는 대표 정보가 아니라 servings 안의 개별 foodId가 사용된다.
         */
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
