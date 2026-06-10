package com.aihealthcoach.meal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.dto.FoodDto.FoodSearchPageResponse;
import com.aihealthcoach.meal.service.FoodService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    @GetMapping
    public ResponseEntity<FoodSearchPageResponse> searchFoodGroups(
            @RequestParam(required = false, defaultValue = "") String query,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(foodService.searchFoodGroups(query, page, size));
    }

    @GetMapping("/search")
    public ResponseEntity<List<FoodCandidateResponse>> searchFoods(
            @RequestParam String query
    ) {
        return ResponseEntity.ok(foodService.searchFoods(query));
    }
}
