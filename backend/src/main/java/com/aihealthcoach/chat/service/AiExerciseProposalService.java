package com.aihealthcoach.chat.service;

import com.aihealthcoach.exercise.dto.AiExerciseDto.ExerciseProposalResponse;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;

public interface AiExerciseProposalService {

    ExerciseProposalResponse createProposal(ExtractedExerciseResult extracted);
}
