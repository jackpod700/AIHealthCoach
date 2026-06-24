package com.aihealthcoach.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExerciseProposalResponse;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.meal.dto.AiMealDto.MealProposalResponse;
import com.aihealthcoach.memory.dto.UserMemoryDto.MemorySaveCommand;
import com.aihealthcoach.weight.dto.AiWeightDto.ExtractedWeightResult;
import com.aihealthcoach.weight.dto.AiWeightDto.WeightProposalResponse;

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
        MealProposalResponse mealProposal,
        ExerciseProposalResponse exerciseProposal,
        WeightProposalResponse weightProposal
    ) {
    }

    public record AiChatResult(
        String assistantMessage,
        ExtractedMealResult mealExtraction,
        ExtractedExerciseResult exerciseExtraction,
        ExtractedWeightResult weightExtraction,
        MemorySaveCommand memorySaveCommand
    ) {
    }

    public record ChatStreamDeltaEvent(
            String content
    ) {
    }

    public record ChatStreamAssistantDoneEvent(
            ChatMessageResponse message
    ) {
    }

    public record ChatStreamToolResultEvent(
            String status,
            MealProposalResponse mealProposal,
            ExerciseProposalResponse exerciseProposal,
            WeightProposalResponse weightProposal,
            ChatStreamMemorySaveResult memorySave,
            String reason
    ) {
        public static ChatStreamToolResultEvent success(
                MealProposalResponse mealProposal,
                ExerciseProposalResponse exerciseProposal,
                WeightProposalResponse weightProposal,
                ChatStreamMemorySaveResult memorySave
        ) {
            return new ChatStreamToolResultEvent(
                    "SUCCESS",
                    mealProposal,
                    exerciseProposal,
                    weightProposal,
                    memorySave == null ? ChatStreamMemorySaveResult.none() : memorySave,
                    null
            );
        }

        public static ChatStreamToolResultEvent failed(String reason) {
            return new ChatStreamToolResultEvent(
                    "FAILED",
                    null,
                    null,
                    null,
                    ChatStreamMemorySaveResult.none(),
                    reason
            );
        }
    }

    public record ChatStreamMemorySaveResult(
            String status,
            String reason
    ) {
        public static ChatStreamMemorySaveResult none() {
            return new ChatStreamMemorySaveResult("NONE", null);
        }

        public static ChatStreamMemorySaveResult saved() {
            return new ChatStreamMemorySaveResult("SAVED", null);
        }

        public static ChatStreamMemorySaveResult failed(String reason) {
            return new ChatStreamMemorySaveResult("FAILED", reason);
        }
    }

    public record ChatStreamErrorEvent(
            String code,
            String message
    ) {
    }

    public record ChatStreamDoneEvent(
            String status
    ) {
        public static ChatStreamDoneEvent done() {
            return new ChatStreamDoneEvent("DONE");
        }
    }

}
