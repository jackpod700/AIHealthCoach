package com.aihealthcoach.chat.dto;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.util.MimeType;

public class LlmDto {

    public record LlmRequest(
            String stableSystemPrompt,
            String dynamicContextPrompt,
            String userMessage,
            List<LlmImage> images
    ) {

        public LlmRequest {
            images = images == null ? List.of() : List.copyOf(images);
        }

        public static LlmRequest text(
                String stableSystemPrompt,
                String dynamicContextPrompt,
                String userMessage
        ) {
            return new LlmRequest(stableSystemPrompt, dynamicContextPrompt, userMessage, List.of());
        }

        public static LlmRequest image(
                String stableSystemPrompt,
                String dynamicContextPrompt,
                String userMessage,
                List<LlmImage> images
        ) {
            return new LlmRequest(stableSystemPrompt, dynamicContextPrompt, userMessage, images);
        }

        public boolean hasImages() {
            return !images.isEmpty();
        }

        public String systemPrompt() {
            if (dynamicContextPrompt == null || dynamicContextPrompt.isBlank()) {
                return stableSystemPrompt;
            }
            return stableSystemPrompt + "\n\n" + dynamicContextPrompt;
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
