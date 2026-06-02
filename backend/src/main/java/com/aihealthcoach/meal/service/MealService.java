package com.aihealthcoach.meal.service;

import java.time.LocalDate;

import com.aihealthcoach.meal.dto.MealDto.CreateMealRequest;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.dto.MealDto.MonthlyMealResponse;

public interface MealService {
    DailyMealResponse findDailyMeals(Long userId, LocalDate date);

    MonthlyMealResponse findMonthlyMeals(Long userId, int year, int month);

    DailyMealResponse createMeal(Long userId, CreateMealRequest request);
}
