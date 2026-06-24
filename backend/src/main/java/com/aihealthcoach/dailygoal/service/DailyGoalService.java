package com.aihealthcoach.dailygoal.service;

import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalRecommendationResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalUpsertRequest;
import java.time.LocalDate;
import java.util.Map;

public interface DailyGoalService {
    Map<String, DailyGoalRecommendationResponse> recommendGoals(Long userId);

    DailyGoalResponse upsertCurrentGoal(Long userId, DailyGoalUpsertRequest request);

    DailyGoalResponse findCurrentGoalIfExists(Long userId);

    DailyGoalProgressResponse findProgress(Long userId, LocalDate date);
}
