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
import com.aihealthcoach.meal.dto.MealDto.MonthlyMealDayResponse;
import com.aihealthcoach.meal.dto.MealDto.MonthlyMealResponse;
import com.aihealthcoach.meal.entity.MealFood;
import com.aihealthcoach.meal.exception.MealException;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.summary.entity.DailyChatSummaryChangeSource;
import com.aihealthcoach.summary.service.DailyChatSummaryStateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private static final Set<String> MEAL_TYPES = Set.of("BREAKFAST", "LUNCH", "DINNER", "SNACK");

    private final MealMapper mealMapper;
    private final DailyChatSummaryStateService dailyChatSummaryStateService;

    @Override
    @Transactional(readOnly = true)
    public DailyMealResponse findDailyMeals(Long userId, LocalDate date) {
        List<MealFood> rows = mealMapper.findDailyMeals(userId, date);
        Map<Long, List<MealFood>> groupedRows = new LinkedHashMap<>();

        for (MealFood row : rows) {
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
    @Transactional(readOnly = true)
    public MonthlyMealResponse findMonthlyMeals(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month must be between 1 and 12");
        }

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.plusMonths(1);
        List<MealFood> rows = mealMapper.findMealsBetween(userId, startDate, endDate);
        Map<LocalDate, List<MealFood>> groupedRows = new LinkedHashMap<>();

        for (MealFood row : rows) {
            groupedRows.computeIfAbsent(row.getMealDate(), ignored -> new ArrayList<>()).add(row);
        }

        List<MonthlyMealDayResponse> days = groupedRows.entrySet().stream()
                .map(entry -> toMonthlyMealDayResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new MonthlyMealResponse(year, month, days);
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

        dailyChatSummaryStateService.markChanged(userId, request.mealDate(), DailyChatSummaryChangeSource.MEAL);

        return findDailyMeals(userId, request.mealDate());
    }

    @Override
    @Transactional
    public void deleteMeal(Long userId, Long mealId) {
        LocalDate mealDate = mealMapper.findMealDateById(userId, mealId);
        if (mealDate == null) {
            throw MealException.mealNotFound();
        }

        int deletedRows = mealMapper.deleteMeal(userId, mealId);

        if (deletedRows == 0) {
            throw MealException.mealNotFound();
        }

        dailyChatSummaryStateService.markChanged(userId, mealDate, DailyChatSummaryChangeSource.MEAL);
    }

    private void validateMealType(String mealType) {
        if (!MEAL_TYPES.contains(mealType)) {
            throw MealException.invalidMealType();
        }
    }

    private void validateItems(List<MealItemRequest> items) {
        Set<Long> foodIds = new HashSet<>();

        for (MealItemRequest item : items) {
            if (!foodIds.add(item.foodId())) {
                throw MealException.invalidMealItem();
            }
        }

        List<Long> existingFoodIds = mealMapper.findFoodIdsByIds(List.copyOf(foodIds));
        if (existingFoodIds.size() != foodIds.size()) {
            throw MealException.foodNotFound();
        }
    }

    private MealResponse toMealResponse(List<MealFood> rows) {
        MealFood firstRow = rows.get(0);
        List<MealItemResponse> items = rows.stream()
                .filter(row -> row.getFoodId() != null)
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

    private MonthlyMealDayResponse toMonthlyMealDayResponse(LocalDate date, List<MealFood> rows) {
        Map<Long, List<MealFood>> groupedRows = new LinkedHashMap<>();
        for (MealFood row : rows) {
            groupedRows.computeIfAbsent(row.getMealId(), ignored -> new ArrayList<>()).add(row);
        }

        List<MealResponse> meals = groupedRows.values().stream()
                .map(this::toMealResponse)
                .toList();
        List<String> mealTypes = meals.stream()
                .map(MealResponse::mealType)
                .distinct()
                .toList();

        return new MonthlyMealDayResponse(
                date,
                meals.size(),
                mealTypes,
                sum(meals, MealResponse::totalCalories),
                sum(meals, MealResponse::totalCarbohydrate),
                sum(meals, MealResponse::totalProtein),
                sum(meals, MealResponse::totalFat)
        );
    }

    private MealItemResponse toMealItemResponse(MealFood row) {
        BigDecimal quantity = defaultZero(row.getQuantity());

        return new MealItemResponse(
                row.getFoodId(),
                row.getFoodName(),
                row.getBrand(),
                row.getServingSize(),
                row.getServingUnit(),
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
