package com.aihealthcoach.chat.service;

import com.aihealthcoach.exercise.dto.AiExerciseDto.ExerciseActivityCandidateResponse;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExerciseProposalResponse;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.exercise.mapper.ExerciseMapper;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiExerciseProposalServiceImpl implements AiExerciseProposalService {

    private static final int CANDIDATE_LIMIT = 5;

    private final ExerciseMapper exerciseMapper;

    @Override
    public ExerciseProposalResponse createProposal(ExtractedExerciseResult extracted) {
        if (extracted == null || !extracted.exerciseIntent() || isBlank(extracted.activityKeyword())) {
            return null;
        }

        List<String> missingFields = new ArrayList<>();
        if (extracted.missingFields() != null) {
            missingFields.addAll(extracted.missingFields());
        }

        String intensityLevel = normalizeIntensityLevel(extracted.intensityLevel(), missingFields);

        if (extracted.exerciseDate() == null && !missingFields.contains("exerciseDate")) {
            missingFields.add("exerciseDate");
        }

        if ((extracted.durationMinutes() == null || extracted.durationMinutes() <= 0)
                && !missingFields.contains("durationMinutes")) {
            missingFields.add("durationMinutes");
        }

        List<ExerciseActivityCandidateResponse> candidates = exerciseMapper
                .findExerciseActivityOptions(extracted.activityKeyword())
                .stream()
                .limit(CANDIDATE_LIMIT)
                .map(ExerciseActivityCandidateResponse::fromEntity)
                .toList();

        return new ExerciseProposalResponse(
                extracted.activityKeyword(),
                intensityLevel,
                extracted.exerciseDate(),
                extracted.durationMinutes(),
                extracted.memo(),
                extracted.confidence(),
                missingFields,
                candidates
        );
    }

    private String normalizeIntensityLevel(String intensityLevel, List<String> missingFields) {
        if ("LOW".equals(intensityLevel) || "MEDIUM".equals(intensityLevel) || "HIGH".equals(intensityLevel)) {
            return intensityLevel;
        }

        if (!missingFields.contains("intensityLevel")) {
            missingFields.add("intensityLevel");
        }

        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
