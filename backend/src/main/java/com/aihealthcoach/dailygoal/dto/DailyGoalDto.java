package com.aihealthcoach.dailygoal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyGoalDto {

    public record DailyGoalRecommendationResponse(
            Integer calorieIntakeGoal,
            Integer exerciseCalorieGoal
    ) {
    }

    public record DailyGoalUpsertRequest(
            @NotBlank(message = "goalType은 필수입니다.")
            @Pattern(regexp = "WEIGHT_LOSS|MAINTENANCE|MUSCLE_GAIN", message = "goalType은 WEIGHT_LOSS, MAINTENANCE, MUSCLE_GAIN 중 하나여야 합니다.")
            String goalType,
            @NotNull(message = "calorieIntakeGoal은 필수입니다.")
            @Positive(message = "calorieIntakeGoal은 1 이상이어야 합니다.")
            Integer calorieIntakeGoal,
            @NotNull(message = "exerciseCalorieGoal은 필수입니다.")
            @PositiveOrZero(message = "exerciseCalorieGoal은 0 이상이어야 합니다.")
            Integer exerciseCalorieGoal
    ) {
    }

    public record DailyGoalResponse(
            String goalType,
            Integer calorieIntakeGoal,
            Integer exerciseCalorieGoal,
            LocalDateTime updatedAt
    ) {
    }

    public record DailyGoalProgressResponse(
            LocalDate date,
            DailyGoalProgressSummaryResponse progress,
            DailyGoalMacroRatioResponse macroRatio
    ) {
    }

    public record DailyGoalProgressSummaryResponse(
            DailyGoalMetricProgressResponse calorieIntake,
            DailyGoalMetricProgressResponse exerciseCalories
    ) {
    }

    public record DailyGoalMetricProgressResponse(
            BigDecimal current,
            BigDecimal goal,
            BigDecimal remaining,
            Integer percent
    ) {
    }

    public record DailyGoalMacroRatioResponse(
            DailyGoalMacroRatioMetricResponse carbohydrate,
            DailyGoalMacroRatioMetricResponse protein,
            DailyGoalMacroRatioMetricResponse fat
    ) {
    }

    public record DailyGoalMacroRatioMetricResponse(
            BigDecimal grams,
            BigDecimal calories,
            Integer percent,
            Integer rangeMin,
            Integer rangeMax,
            String status
    ) {
    }
}
