package com.aihealthcoach.meal.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.meal.dto.MealDto.CreateMealRequest;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.dto.MealDto.MealItemResponse;
import com.aihealthcoach.meal.dto.MealDto.MealItemRequest;
import com.aihealthcoach.meal.dto.MealDto.MealResponse;
import com.aihealthcoach.meal.dto.MealFoodRow;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.MealMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private static final Set<String> MEAL_TYPES = Set.of("BREAKFAST", "LUNCH", "DINNER", "SNACK");

    private final MealMapper mealMapper;

    @Override
    @Transactional(readOnly = true)
    public DailyMealResponse findDailyMeals(Long userId, LocalDate date) {
        List<MealFoodRow> rows = mealMapper.findDailyMeals(userId, date);
        Map<Long, List<MealFoodRow>> groupedRows = new LinkedHashMap<>();

        for (MealFoodRow row : rows) {
            groupedRows.computeIfAbsent(row.getMealId(), ignored -> new ArrayList<>()).add(row);
        }

        List<MealResponse> meals = groupedRows.values().stream()
                .map(this::toMealResponse)
                .toList();

        return new DailyMealResponse(
                date,
                meals,
                sum(meals, MealResponse::totalCalories),
                sum(meals, MealResponse::totalCarbohydrate),
                sum(meals, MealResponse::totalProtein),
                sum(meals, MealResponse::totalFat)
        );
    }

    @Override
    @Transactional
    public DailyMealResponse createMeal(Long userId, CreateMealRequest request) {
        validateMealType(request.mealType());
        validateItems(request.items());

        Long mealId = mealMapper.findMealIdByUserTypeDate(userId, request.mealType(), request.mealDate());

        if (mealId == null) {
            mealId = mealMapper.insertMeal(userId, request.mealType(), request.mealDate());
        } else {
            mealMapper.deleteMealItems(mealId);
        }

        for (MealItemRequest item : request.items()) {
            mealMapper.insertMealItem(mealId, item);
        }

        return findDailyMeals(userId, request.mealDate());
    }

    private void validateMealType(String mealType) {
        if (!MEAL_TYPES.contains(mealType)) {
            throw MealException.invalidMealType();
        }
    }

    private void validateItems(List<MealItemRequest> items) {
        Set<String> foodCodes = new HashSet<>();

        for (MealItemRequest item : items) {
            if (!foodCodes.add(item.foodCode())) {
                throw MealException.invalidMealItem();
            }
            if (!mealMapper.existsFoodCode(item.foodCode())) {
                throw MealException.foodNotFound();
            }
        }
    }

    private MealResponse toMealResponse(List<MealFoodRow> rows) {
        MealFoodRow firstRow = rows.get(0);
        List<MealItemResponse> items = rows.stream()
                .filter(row -> row.getFoodCode() != null)
                .map(this::toMealItemResponse)
                .toList();

        return new MealResponse(
                firstRow.getMealId(),
                firstRow.getMealType(),
                items,
                sumItems(items, MealItemResponse::calories),
                sumItems(items, MealItemResponse::carbohydrate),
                sumItems(items, MealItemResponse::protein),
                sumItems(items, MealItemResponse::fat)
        );
    }

    private MealItemResponse toMealItemResponse(MealFoodRow row) {
        BigDecimal quantity = defaultZero(row.getQuantity());

        return new MealItemResponse(
                row.getFoodCode(),
                row.getFoodName(),
                row.getManufacturer(),
                quantity,
                multiply(defaultZero(row.getCalories()), quantity),
                multiply(defaultZero(row.getCarbohydrate()), quantity),
                multiply(defaultZero(row.getProtein()), quantity),
                multiply(defaultZero(row.getFat()), quantity)
        );
    }

    private BigDecimal multiply(BigDecimal nutrient, BigDecimal quantity) {
        return nutrient.multiply(quantity);
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal sum(List<MealResponse> meals, java.util.function.Function<MealResponse, BigDecimal> mapper) {
        return meals.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumItems(List<MealItemResponse> items, java.util.function.Function<MealItemResponse, BigDecimal> mapper) {
        return items.stream()
                .map(mapper)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
