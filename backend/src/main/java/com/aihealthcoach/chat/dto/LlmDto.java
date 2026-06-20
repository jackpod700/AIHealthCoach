package com.aihealthcoach.chat.dto;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;

public class LlmDto {

    public record LlmRequest(
            String systemPrompt,
            String userMessage,
            List<LlmImage> images,
            UserChatContext context
    ) {

        public LlmRequest {
            images = images == null ? List.of() : List.copyOf(images);
        }

        public static LlmRequest text(String systemPrompt, String userMessage, UserChatContext context) {
            return new LlmRequest(systemPrompt, userMessage, List.of(), context);
        }

        public static LlmRequest image(
                String systemPrompt,
                String userMessage,
                List<LlmImage> images,
                UserChatContext context
        ) {
            return new LlmRequest(systemPrompt, userMessage, images, context);
        }

        public boolean hasImages() {
            return !images.isEmpty();
        }
    }

    public record LlmImage(
            MimeType mimeType,
            Resource resource
    ) {
    }

    public record LlmResponse(
            String content
    ) {
    }
}
