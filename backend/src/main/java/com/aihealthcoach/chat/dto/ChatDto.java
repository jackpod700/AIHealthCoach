package com.aihealthcoach.chat.dto;

import java.time.LocalDateTime;

import com.aihealthcoach.chat.entity.ChatMessage;

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
        Long id,
        String content
    ){
        public ChatMessage toEntity(){
                return ChatMessage.builder()
                                  .userId(1L)//수정필요
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

}
