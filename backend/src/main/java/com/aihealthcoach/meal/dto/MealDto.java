package com.aihealthcoach.meal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class MealDto {

    public record CreateMealRequest(
            @NotNull LocalDate mealDate,
            @NotBlank String mealType,
            @NotEmpty List<@Valid MealItemRequest> items
    ) {
    }

    public record MealItemRequest(
            @NotBlank String foodCode,
            @NotNull @Positive BigDecimal quantity
    ) {
    }

    public record DailyMealResponse(
            LocalDate date,
            List<MealResponse> meals,
            BigDecimal dailyTotalCalories,
            BigDecimal dailyTotalCarbohydrate,
            BigDecimal dailyTotalProtein,
            BigDecimal dailyTotalFat
    ) {
    }

    public record MonthlyMealResponse(
            int year,
            int month,
            List<MonthlyMealDayResponse> days
    ) {
    }

    public record MonthlyMealDayResponse(
            LocalDate date,
            int mealCount,
            List<String> mealTypes,
            BigDecimal totalCalories,
            BigDecimal totalCarbohydrate,
            BigDecimal totalProtein,
            BigDecimal totalFat
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
