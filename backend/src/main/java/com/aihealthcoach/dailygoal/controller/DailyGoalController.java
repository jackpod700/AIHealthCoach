package com.aihealthcoach.dailygoal.controller;

import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalRecommendationResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalUpsertRequest;
import com.aihealthcoach.dailygoal.service.DailyGoalService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/daily-goals")
@RequiredArgsConstructor
public class DailyGoalController {

    private final DailyGoalService dailyGoalService;

    @GetMapping("/recommendation")
    public ResponseEntity<DailyGoalRecommendationResponse> recommendGoal(
            @RequestParam String goalType,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(dailyGoalService.recommendGoal(userId, goalType));
    }

    @PutMapping("/confirm")
    public ResponseEntity<DailyGoalResponse> upsertCurrentGoal(
            @Valid @RequestBody DailyGoalUpsertRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(dailyGoalService.upsertCurrentGoal(userId, request));
    }

    @GetMapping("/progress")
    public ResponseEntity<DailyGoalProgressResponse> findProgress(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(dailyGoalService.findProgress(userId, date));
    }
}
