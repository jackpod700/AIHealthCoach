package com.aihealthcoach.chat.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.exception.ChatException;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L;
    private static final long MAX_TOTAL_IMAGE_SIZE_BYTES = 50L * 1024L;
    private static final String DEFAULT_IMAGE_MESSAGE = "사진을 분석해서 식단 후보를 만들어줘.";

    private final AiChatClientGateway aiChatClientGateway;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AiPromptFactory promptFactory;


    @Override
    public AiChatResult generate(Long userId, ChatMessageRequest userMessage) {
        try {
            /*
             * AI 사용량 기록은 gateway 메서드에 붙은 AOP에서 처리한다.
             * 이 서비스는 AI 응답을 도메인에서 쓰기 좋은 형태로 정규화하는 책임만 가진다.
             */
            ResponseEntity<ChatResponse, AiChatResult> response = aiChatClientGateway.callTextChat(
                    userId,
                    systemPrompt(LocalDate.now(clock)),
                    userMessage.content()
            );
            return normalizeAiResult(response.entity());
        } catch (Exception exception) {
            log.warn("Failed to map AI chat response to AiChatResult.", exception);
            return fallback();
        }
    }

    @Override
    public AiChatResult generateWithImages(Long userId, String content, List<MultipartFile> images) {
        validateImages(images);

        String userText = normalizeImageMessage(content);
        try {
            /*
             * 이미지 AI 호출도 gateway를 통과하므로 텍스트 채팅과 같은 AOP 로깅 흐름을 사용한다.
             */
            ResponseEntity<ChatResponse, AiChatResult> response = aiChatClientGateway.callImageMeal(
                    userId,
                    promptFactory.imageMealPrompt(LocalDate.now(clock)),
                    userText,
                    images
            );
            return normalizeAiResult(response.entity());
        } catch (Exception exception) {
            log.warn("Failed to map AI image response to AiChatResult.", exception);
            return fallback();
        }
    }

    String systemPrompt(LocalDate today) {
        return promptFactory.textChatPrompt(today);
    }

    AiChatResult parseAiResult(String content) {
        try {
            objectMapper.findAndRegisterModules();
            AiChatResult result = objectMapper.readValue(content, AiChatResult.class);
            return normalizeAiResult(result);
        } catch (Exception exception) {
            return fallback();
        }
    }

    private AiChatResult normalizeAiResult(AiChatResult result) {
        if (result == null || result.assistantMessage() == null || result.assistantMessage().isBlank()) {
            return fallback();
        }

        return new AiChatResult(
                result.assistantMessage(),
                result.mealExtraction() == null ? ExtractedMealResult.noMeal() : result.mealExtraction(),
                result.exerciseExtraction() == null ? ExtractedExerciseResult.noExercise() : result.exerciseExtraction()
        );
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

    private String normalizeImageMessage(String content) {
        if (content == null || content.isBlank()) {
            return DEFAULT_IMAGE_MESSAGE;
        }
        return content.trim();
    }
}
