package com.aihealthcoach.meal.service;

import java.time.LocalDate;

import com.aihealthcoach.meal.dto.MealDto.CreateMealRequest;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;

public interface MealService {
    DailyMealResponse findDailyMeals(Long userId, LocalDate date);

    DailyMealResponse createMeal(Long userId, CreateMealRequest request);
}
