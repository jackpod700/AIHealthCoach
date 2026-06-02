package com.aihealthcoach.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.meal.dto.AiMealDto.MealProposalResponse;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class ChatDto {

    public record AiChatRequest(
            @NotBlank(message = "message는 필수입니다.")
            String message
    ) {
    }

    @Builder
    public record AiChatResponse(
            String userMessage,
            String aiMessage
    ) {
    }

    @Builder
    public record ChatMessageRequest(
        String content
    ){
        public ChatMessage toEntity(Long userId){
                return ChatMessage.builder()
                                  .userId(userId)
                                  .role("USER")
                                  .content(this.content())
                                  .build();
        }

    }

    @Builder
    public record ChatMessageResponse(
        String role,
        String content,
        LocalDateTime createdAt
    ){
        public static ChatMessageResponse fromEntity(ChatMessage entity){
                return ChatMessageResponse.builder()
                                          .role(entity.getRole())
                                          .content(entity.getContent())
                                          .createdAt(entity.getCreatedAt())
                                          .build();
        }
    }

    public record ChatMessageSendResponse(
            List<ChatMessageResponse> messages,
            MealProposalResponse mealProposal
    ) {
    }

    public record AiChatResult(
            String assistantMessage,
            ExtractedMealResult mealExtraction
    ) {
    }

}
