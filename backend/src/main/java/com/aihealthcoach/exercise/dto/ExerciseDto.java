package com.aihealthcoach.exercise.dto;

import com.aihealthcoach.exercise.entity.ExerciseRecord;
import com.aihealthcoach.exercise.entity.ExerciseActivityOption;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;

public class ExerciseDto {

    @Builder
    public record ExerciseActivityOptionResponse(
        Long id,
        String activityNameKo,
        String majorHeading,
        IntensityMetResponse low,
        IntensityMetResponse medium,
        IntensityMetResponse high) {
        public static ExerciseActivityOptionResponse fromEntity(ExerciseActivityOption entity) {
            return ExerciseActivityOptionResponse.builder()
                .id(entity.getId())
                .activityNameKo(entity.getActivityNameKo())
                .majorHeading(entity.getMajorHeading())
                .low(IntensityMetResponse.builder()
                    .metValue(entity.getLowMetValue())
                    .metSource(entity.getLowMetSource())
                    .sourceDescription(entity.getLowSourceDescription())
                    .build())
                .medium(IntensityMetResponse.builder()
                    .metValue(entity.getMediumMetValue())
                    .metSource(entity.getMediumMetSource())
                    .sourceDescription(entity.getMediumSourceDescription())
                    .build())
                .high(IntensityMetResponse.builder()
                    .metValue(entity.getHighMetValue())
                    .metSource(entity.getHighMetSource())
                    .sourceDescription(entity.getHighSourceDescription())
                    .build())
                .build();
        }
    }

    @Builder
    public record IntensityMetResponse(
        BigDecimal metValue,
        String metSource,
        String sourceDescription) {
    }

    @Builder
    public record ExerciseRecordRequest(
        @NotNull(message = "exerciseActivityOptionId는 필수입니다.") Long exerciseActivityOptionId,
        @NotNull(message = "intensityLevel은 필수입니다.")
        @Pattern(regexp = "LOW|MEDIUM|HIGH", message = "intensityLevel은 LOW, MEDIUM, HIGH 중 하나여야 합니다.")
        String intensityLevel,
        @NotNull(message = "exerciseDate는 필수입니다.") LocalDate exerciseDate,
        @NotNull(message = "durationMinutes는 필수입니다.") @Positive(message = "durationMinutes는 1 이상이어야 합니다.") Integer durationMinutes,
        String memo) {
    }

    @Builder
    public record ExerciseRecordResponse(
        Long id,
        Long exerciseActivityOptionId,
        String activityNameKo,
        String intensityLevel,
        BigDecimal metValue,
        String metSource,
        String sourceDescription,
        LocalDate exerciseDate,
        Integer durationMinutes,
        Integer caloriesBurned,
        String memo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
        public static ExerciseRecordResponse fromEntity(ExerciseRecord entity) {
            ExerciseActivityOption option = entity.getExerciseActivityOption();
            IntensityMetResponse intensityMet = resolveIntensityMet(option, entity.getIntensityLevel());

            return ExerciseRecordResponse.builder()
                .id(entity.getId())
                .exerciseActivityOptionId(entity.getExerciseActivityOptionId())
                .activityNameKo(option == null ? null : option.getActivityNameKo())
                .intensityLevel(entity.getIntensityLevel())
                .metValue(intensityMet == null ? null : intensityMet.metValue())
                .metSource(intensityMet == null ? null : intensityMet.metSource())
                .sourceDescription(intensityMet == null ? null : intensityMet.sourceDescription())
                .exerciseDate(entity.getExerciseDate())
                .durationMinutes(entity.getDurationMinutes())
                .caloriesBurned(entity.getCaloriesBurned())
                .memo(entity.getMemo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
        }

        private static IntensityMetResponse resolveIntensityMet(ExerciseActivityOption option, String intensityLevel) {
            if (option == null || intensityLevel == null) {
                return null;
            }

            return switch (intensityLevel) {
                case "LOW" -> IntensityMetResponse.builder()
                    .metValue(option.getLowMetValue())
                    .metSource(option.getLowMetSource())
                    .sourceDescription(option.getLowSourceDescription())
                    .build();
                case "MEDIUM" -> IntensityMetResponse.builder()
                    .metValue(option.getMediumMetValue())
                    .metSource(option.getMediumMetSource())
                    .sourceDescription(option.getMediumSourceDescription())
                    .build();
                case "HIGH" -> IntensityMetResponse.builder()
                    .metValue(option.getHighMetValue())
                    .metSource(option.getHighMetSource())
                    .sourceDescription(option.getHighSourceDescription())
                    .build();
                default -> null;
            };
        }
    }
}
