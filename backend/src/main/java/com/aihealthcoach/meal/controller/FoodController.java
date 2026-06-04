package com.aihealthcoach.meal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.mapper.MealMapper;
import com.aihealthcoach.meal.util.FoodSearchQuery;
import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final MealMapper mealMapper;

    @GetMapping("/search")
    public ResponseEntity<List<FoodCandidateResponse>> searchFoods(
            @RequestParam String query
    ) {
        String trimmedQuery = FoodSearchQuery.normalize(query);
        List<Token> tokens = FoodSearchQuery.tokens(trimmedQuery);

        if (tokens.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<FoodCandidateResponse> foods = mealMapper.searchFoods(trimmedQuery, tokens).stream()
                .map(FoodCandidateResponse::fromRow)
                .toList();

        return ResponseEntity.ok(foods);
    }
}
