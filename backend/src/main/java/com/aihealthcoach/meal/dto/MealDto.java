package com.aihealthcoach.meal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class MealDto {

    public record DailyMealResponse(
            LocalDate date,
            List<MealResponse> meals,
            BigDecimal dailyTotalCalories,
            BigDecimal dailyTotalCarbohydrate,
            BigDecimal dailyTotalProtein,
            BigDecimal dailyTotalFat
    ) {
    }

    public record MealResponse(
            Long mealId,
            String mealType,
            List<MealItemResponse> items,
            BigDecimal totalCalories,
            BigDecimal totalCarbohydrate,
            BigDecimal totalProtein,
            BigDecimal totalFat
    ) {
    }

    public record MealItemResponse(
            String foodCode,
            String foodName,
            String manufacturer,
            BigDecimal quantity,
            BigDecimal calories,
            BigDecimal carbohydrate,
            BigDecimal protein,
            BigDecimal fat
    ) {
    }
}
