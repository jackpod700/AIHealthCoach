package com.aihealthcoach.chat.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalRequest;
import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalResponse;
import com.aihealthcoach.meal.dto.MealDto.CreateMealRequest;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.dto.MealDto.MealItemRequest;
import com.aihealthcoach.meal.service.MealService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatMealProposalServiceImpl implements ChatMealProposalService {

    private final MealService mealService;
    private final ChatService chatService;

    public ConfirmMealProposalResponse confirm(Long userId, ConfirmMealProposalRequest request) {
        CreateMealRequest createMealRequest = new CreateMealRequest(
                request.mealDate(),
                request.mealType(),
                request.items().stream()
                        .map(item -> new MealItemRequest(item.foodId(), item.quantity()))
                        .toList()
        );
        DailyMealResponse dailyMeal = mealService.createMeal(userId, createMealRequest);
        ChatMessageResponse confirmationMessage = chatService.insert(ChatMessage.builder()
                .userId(userId)
                .role("ASSISTANT")
                .content(toConfirmationMessage(request.mealType()))
                .build());

        return new ConfirmMealProposalResponse(List.of(confirmationMessage), dailyMeal);
    }

    private String toConfirmationMessage(String mealType) {
        return switch (mealType) {
            case "BREAKFAST" -> "아침 식단으로 기록할게요.";
            case "LUNCH" -> "점심 식단으로 기록할게요.";
            case "DINNER" -> "저녁 식단으로 기록할게요.";
            case "SNACK" -> "간식 식단으로 기록할게요.";
            default -> "식단으로 기록할게요.";
        };
    }
}
