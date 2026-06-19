package com.aihealthcoach.chat.dto;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

public class LlmDto {

    public record LlmRequest(
            String systemPrompt,
            String userMessage,
            List<LlmImage> images
    ) {

        public LlmRequest {
            images = images == null ? List.of() : List.copyOf(images);
        }

        public static LlmRequest text(String systemPrompt, String userMessage) {
            return new LlmRequest(systemPrompt, userMessage, List.of());
        }

        public static LlmRequest image(String systemPrompt, String userMessage, List<LlmImage> images) {
            return new LlmRequest(systemPrompt, userMessage, images);
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
