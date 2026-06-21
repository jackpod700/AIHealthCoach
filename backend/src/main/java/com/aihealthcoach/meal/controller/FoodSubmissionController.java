package com.aihealthcoach.meal.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionCreateRequest;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionPageResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionResponse;
import com.aihealthcoach.meal.service.FoodSubmissionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/foods/requests")
@RequiredArgsConstructor
@Validated
public class FoodSubmissionController {

    private final FoodSubmissionService foodSubmissionService;

    @PostMapping
    public ResponseEntity<FoodSubmissionResponse> createSubmission(
            Authentication authentication,
            @Valid @RequestBody FoodSubmissionCreateRequest request
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(foodSubmissionService.createSubmission(userId, request));
    }

    @GetMapping("/my")
    public ResponseEntity<FoodSubmissionPageResponse> findMySubmissions(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(foodSubmissionService.findMySubmissions(userId, page, size));
    }
}
