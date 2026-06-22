package com.aihealthcoach.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.admin.service.AdminFoodSubmissionService;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionPageResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionRejectRequest;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionResponse;
import com.aihealthcoach.meal.dto.FoodSubmissionDto.FoodSubmissionReviewRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/food-requests")
@RequiredArgsConstructor
@Validated
public class AdminFoodSubmissionController {

    private final AdminFoodSubmissionService adminFoodSubmissionService;

    @GetMapping
    public ResponseEntity<FoodSubmissionPageResponse> findRequests(
            @RequestParam(required = false, defaultValue = "PENDING") String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminFoodSubmissionService.findRequests(status, page, size));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<FoodSubmissionResponse> findRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(adminFoodSubmissionService.findRequest(requestId));
    }

    @PatchMapping("/{requestId}/approve")
    public ResponseEntity<FoodSubmissionResponse> approveRequest(
            Authentication authentication,
            @PathVariable Long requestId,
            @Valid @RequestBody FoodSubmissionReviewRequest request
    ) {
        Long adminId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminFoodSubmissionService.approveRequest(adminId, requestId, request));
    }

    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<FoodSubmissionResponse> rejectRequest(
            Authentication authentication,
            @PathVariable Long requestId,
            @Valid @RequestBody FoodSubmissionRejectRequest request
    ) {
        Long adminId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(adminFoodSubmissionService.rejectRequest(adminId, requestId, request));
    }
}
