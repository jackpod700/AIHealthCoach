package com.aihealthcoach.meal.service;

import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.meal.dto.AiMealDto.MealProposalResponse;

public interface AiMealProposalService {
    MealProposalResponse createProposal(ExtractedMealResult extracted);
}
