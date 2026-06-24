package com.aihealthcoach.dailygoal.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyGoal {
    private Long id;
    private Long userId;
    private String goalType;
    private Integer calorieIntakeGoal;
    private Integer exerciseCalorieGoal;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
