package com.aihealthcoach.exercise.dto;

import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.entity.PhysicalActivity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

public class ExerciseDto {

    @Builder
    public record PhysicalActivityResponse(
        Long id, 
        String compendiumCode, 
        String compendiumVersion,
        String majorHeading,
        BigDecimal metValue, 
        String description) {
        public static PhysicalActivityResponse fromEntity(PhysicalActivity entity) {
            return PhysicalActivityResponse.builder()
                .id(entity.getId())
                .compendiumCode(entity.getCompendiumCode())
                .compendiumVersion(entity.getCompendiumVersion())
                .majorHeading(entity.getMajorHeading())
                .metValue(entity.getMetValue())
                .description(entity.getDescription()).build();
        }
    }

    @Builder
    public record ExerciseRecordRequest(
        @NotNull(message = "physicalActivityId는 필수입니다.") Long physicalActivityId,
        @NotNull(message = "exerciseDate는 필수입니다.") LocalDate exerciseDate,
        @NotNull(message = "durationMinutes는 필수입니다.") @Positive(message = "durationMinutes는 1 이상이어야 합니다.") Integer durationMinutes,
        String memo) {
    }

    @Builder
    public record ExerciseRecordResponse(
            Long id, 
            Long physicalActivityId, 
            String compendiumCode,
            String compendiumVersion,
            String majorHeading, 
            BigDecimal metValue, 
            String description,
            LocalDate exerciseDate, 
            Integer durationMinutes, 
            Integer caloriesBurned, 
            String memo,
            LocalDateTime createdAt, 
            LocalDateTime updatedAt) {
        public static ExerciseRecordResponse fromEntity(ExerciseRecord entity) {
            PhysicalActivity activity = entity.getPhysicalActivity();

            return ExerciseRecordResponse.builder().id(entity.getId())
                    .physicalActivityId(entity.getPhysicalActivityId())
                    .compendiumCode(activity == null ? null : activity.getCompendiumCode())
                    .compendiumVersion(activity == null ? null : activity.getCompendiumVersion())
                    .majorHeading(activity == null ? null : activity.getMajorHeading())
                    .metValue(activity == null ? null : activity.getMetValue())
                    .description(activity == null ? null : activity.getDescription())
                    .exerciseDate(entity.getExerciseDate()).durationMinutes(entity.getDurationMinutes())
                    .caloriesBurned(entity.getCaloriesBurned()).memo(entity.getMemo()).createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt()).build();
        }
    }
}
