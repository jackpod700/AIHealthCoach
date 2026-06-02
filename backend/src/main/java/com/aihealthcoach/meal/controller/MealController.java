package com.aihealthcoach.meal.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.meal.dto.MealDto.CreateMealRequest;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.dto.MealDto.MonthlyMealResponse;
import com.aihealthcoach.meal.service.MealService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
@Validated
public class MealController {

    private final MealService mealService;

    @GetMapping("/daily")
    public ResponseEntity<DailyMealResponse> getDailyMeals(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(mealService.findDailyMeals(userId, date));
    }

    @GetMapping("/monthly")
    public ResponseEntity<MonthlyMealResponse> getMonthlyMeals(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam @Min(1) @Max(12) int month
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(mealService.findMonthlyMeals(userId, year, month));
    }

    @PostMapping
    public ResponseEntity<DailyMealResponse> createMeal(
            Authentication authentication,
            @Valid @RequestBody CreateMealRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(mealService.createMeal(userId, request));
    }
}
