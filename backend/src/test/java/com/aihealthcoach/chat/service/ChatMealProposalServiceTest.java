package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalItemRequest;
import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalRequest;
import com.aihealthcoach.meal.dto.MealDto.CreateMealRequest;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.service.MealService;

@ExtendWith(MockitoExtension.class)
class ChatMealProposalServiceTest {

    @Mock
    private MealService mealService;

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatMealProposalService chatMealProposalService;

    @Test
    void confirmSavesMealAndAssistantConfirmationMessage() {
        Long userId = 1L;
        LocalDate mealDate = LocalDate.of(2026, 6, 2);
        ConfirmMealProposalRequest request = new ConfirmMealProposalRequest(
                mealDate,
                "LUNCH",
                List.of(new ConfirmMealProposalItemRequest("1234567890123456789", new BigDecimal("1")))
        );
        DailyMealResponse dailyMeal = new DailyMealResponse(
                mealDate,
                List.of(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        var savedMessage = new com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse(
                "ASSISTANT",
                "점심 식단으로 기록했어요.",
                null
        );

        when(mealService.createMeal(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any(CreateMealRequest.class)))
                .thenReturn(dailyMeal);
        when(chatService.insert(org.mockito.ArgumentMatchers.any(ChatMessage.class))).thenReturn(savedMessage);

        var response = chatMealProposalService.confirm(userId, request);

        assertThat(response.dailyMeal()).isSameAs(dailyMeal);
        assertThat(response.messages()).containsExactly(savedMessage);
        verify(mealService).createMeal(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any(CreateMealRequest.class));
        verify(chatService).insert(org.mockito.ArgumentMatchers.argThat(message ->
                message.getUserId().equals(userId)
                        && message.getRole().equals("ASSISTANT")
                        && message.getContent().contains("점심")
        ));
    }
}
