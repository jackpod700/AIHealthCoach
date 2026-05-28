package com.aihealthcoach.chat.dto;

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
}
