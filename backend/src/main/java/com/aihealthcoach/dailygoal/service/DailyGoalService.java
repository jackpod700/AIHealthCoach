package com.aihealthcoach.dailygoal.service;

import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalProgressResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalRecommendationResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalResponse;
import com.aihealthcoach.dailygoal.dto.DailyGoalDto.DailyGoalUpsertRequest;
import java.time.LocalDate;

public interface DailyGoalService {
    DailyGoalRecommendationResponse recommendGoal(Long userId, String goalType);

    DailyGoalResponse upsertCurrentGoal(Long userId, DailyGoalUpsertRequest request);

    DailyGoalProgressResponse findProgress(Long userId, LocalDate date);
}
