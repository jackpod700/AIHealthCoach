package com.aihealthcoach.meal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissCreateRequest;
import com.aihealthcoach.meal.dto.FoodSearchMissDto.FoodSearchMissResponse;
import com.aihealthcoach.meal.service.FoodSearchMissService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/foods/search-misses")
@RequiredArgsConstructor
@Validated
public class FoodSearchMissController {

    private final FoodSearchMissService foodSearchMissService;

    @PostMapping
    public ResponseEntity<FoodSearchMissResponse> recordMiss(
            Authentication authentication,
            @RequestBody FoodSearchMissCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(foodSearchMissService.recordMiss(userId, request));
    }
}
