package com.aihealthcoach.chat.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.exception.ChatException;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L * 1024L;
    private static final long MAX_TOTAL_IMAGE_SIZE_BYTES = 50L * 1024L * 1024L;
    private static final String DEFAULT_IMAGE_MESSAGE = "사진을 분석해서 식단 후보를 만들어줘.";

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AiPromptFactory promptFactory;


    @Override
    public AiChatResult generate(ChatMessageRequest userMessage) {
        String content = chatClient.prompt(systemPrompt(LocalDate.now(clock)))
                .user(userMessage.content())
                .call()
                .content();

        return parseAiResult(content);
    }

    @Override
    public AiChatResult generateWithImages(String content, List<MultipartFile> images) {
        validateImages(images);

        String userText = normalizeImageMessage(content);
        String responseContent = chatClient.prompt(promptFactory.imageMealPrompt(LocalDate.now(clock)))
                .user(user -> {
                    user.text(userText);
                    images.forEach(image -> user.media(toMimeType(image), image.getResource()));
                })
                .call()
                .content();

        return parseAiResult(responseContent);
    }

    String systemPrompt(LocalDate today) {
        return promptFactory.textChatPrompt(today);
    }

    AiChatResult parseAiResult(String content) {
        try {
            objectMapper.findAndRegisterModules();
            AiChatResult result = objectMapper.readValue(content, AiChatResult.class);
            if (result.assistantMessage() == null || result.assistantMessage().isBlank()) {
                return fallback();
            }
            if (result.mealExtraction() == null) {
                return new AiChatResult(
                        result.assistantMessage(),
                        ExtractedMealResult.noMeal(),
                        result.exerciseExtraction() == null ? ExtractedExerciseResult.noExercise() : result.exerciseExtraction()
                );
            }
            if (result.exerciseExtraction() == null) {
                return new AiChatResult(result.assistantMessage(), result.mealExtraction(), ExtractedExerciseResult.noExercise());
            }
            return result;
        } catch (Exception exception) {
            return fallback();
        }
    }

    private AiChatResult fallback() {
        return new AiChatResult(
                "응답을 정리하지 못했어요. 다시 한 번 자연스럽게 말해 주세요.",
                ExtractedMealResult.noMeal(),
                ExtractedExerciseResult.noExercise()
        );
    }

    private void validateImages(List<MultipartFile> images) {
        if (images == null || images.isEmpty() || images.stream().allMatch(MultipartFile::isEmpty)) {
            throw ChatException.imageRequired();
        }

        long totalSize = 0;
        for (MultipartFile image : images) {
            if (image == null || image.isEmpty()) {
                throw ChatException.imageRequired();
            }
            if (!isSupportedImageType(image.getContentType())) {
                throw ChatException.unsupportedImageType();
            }
            if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw ChatException.imageTooLarge();
            }
            totalSize += image.getSize();
        }

        if (totalSize > MAX_TOTAL_IMAGE_SIZE_BYTES) {
            throw ChatException.imageTotalTooLarge();
        }
    }

    private boolean isSupportedImageType(String contentType) {
        return MediaType.IMAGE_JPEG_VALUE.equals(contentType)
                || MediaType.IMAGE_PNG_VALUE.equals(contentType)
                || "image/webp".equals(contentType);
    }

    private MimeType toMimeType(MultipartFile image) {
        return MimeTypeUtils.parseMimeType(image.getContentType());
    }

    private String normalizeImageMessage(String content) {
        if (content == null || content.isBlank()) {
            return DEFAULT_IMAGE_MESSAGE;
        }
        return content.trim();
    }
}
