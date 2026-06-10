package com.aihealthcoach.exercise.dto;

import com.aihealthcoach.exercise.dto.ExerciseDto.IntensityMetResponse;
import com.aihealthcoach.exercise.entity.ExerciseActivityOption;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AiExerciseDto {

    public record ExtractedExerciseResult(
            boolean exerciseIntent,
            String activityKeyword,
            String intensityLevel,
            LocalDate exerciseDate,
            Integer durationMinutes,
            String memo,
            BigDecimal confidence,
            List<String> missingFields
    ) {
        public static ExtractedExerciseResult noExercise() {
            return new ExtractedExerciseResult(false, null, null, null, null, null, null, List.of());
        }
    }

    public record ExerciseProposalResponse(
            String activityKeyword,
            String intensityLevel,
            LocalDate exerciseDate,
            Integer durationMinutes,
            String memo,
            BigDecimal confidence,
            List<String> missingFields,
            List<ExerciseActivityCandidateResponse> candidates
    ) {
    }

    public record ExerciseActivityCandidateResponse(
            Long id,
            String activityNameKo,
            String majorHeading,
            IntensityMetResponse low,
            IntensityMetResponse medium,
            IntensityMetResponse high
    ) {
        public static ExerciseActivityCandidateResponse fromEntity(ExerciseActivityOption entity) {
            return new ExerciseActivityCandidateResponse(
                    entity.getId(),
                    entity.getActivityNameKo(),
                    entity.getMajorHeading(),
                    IntensityMetResponse.builder()
                            .metValue(entity.getLowMetValue())
                            .metSource(entity.getLowMetSource())
                            .sourceDescription(entity.getLowSourceDescription())
                            .build(),
                    IntensityMetResponse.builder()
                            .metValue(entity.getMediumMetValue())
                            .metSource(entity.getMediumMetSource())
                            .sourceDescription(entity.getMediumSourceDescription())
                            .build(),
                    IntensityMetResponse.builder()
                            .metValue(entity.getHighMetValue())
                            .metSource(entity.getHighMetSource())
                            .sourceDescription(entity.getHighSourceDescription())
                            .build()
            );
        }
    }
}
