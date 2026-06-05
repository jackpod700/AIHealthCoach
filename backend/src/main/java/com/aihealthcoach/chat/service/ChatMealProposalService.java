package com.aihealthcoach.chat.service;

import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalRequest;
import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalResponse;

public interface ChatMealProposalService {
    public ConfirmMealProposalResponse confirm(Long userId, ConfirmMealProposalRequest request);
}
