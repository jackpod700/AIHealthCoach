package com.aihealthcoach.exercise.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseRecord {
    private Long id;
    private Long userId;
    private Long exerciseActivityOptionId;
    private String intensityLevel;
    private LocalDate exerciseDate;
    private Integer durationMinutes;
    private Integer caloriesBurned;
    private String memo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ExerciseActivityOption exerciseActivityOption;
}
