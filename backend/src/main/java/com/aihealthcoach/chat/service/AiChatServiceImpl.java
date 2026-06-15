package com.aihealthcoach.chat.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aihealthcoach.admin.entity.AiUsageLog;
import com.aihealthcoach.admin.mapper.AdminMapper;
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

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AiPromptFactory promptFactory;
    private final AdminMapper adminMapper;

    @Value("${spring.ai.openai.chat.options.model:unknown}")
    private String configuredModel;


    @Override
    public AiChatResult generate(Long userId, ChatMessageRequest userMessage) {
        long startedAt = System.nanoTime();
        try {
            org.springframework.ai.chat.client.ResponseEntity<ChatResponse, AiChatResult> response = chatClient.prompt(systemPrompt(LocalDate.now(clock)))
                    .user(userMessage.content())
                    .call()
                    .responseEntity(AiChatResult.class);

            recordAiUsage(userId, "TEXT_CHAT", startedAt, true, null, response.response());
            return normalizeAiResult(response.entity());
        } catch (Exception exception) {
            log.warn("Failed to map AI chat response to AiChatResult.", exception);
            recordAiUsage(userId, "TEXT_CHAT", startedAt, false, exception, null);
            return fallback();
        }
    }

    @Override
    public AiChatResult generateWithImages(Long userId, String content, List<MultipartFile> images) {
        validateImages(images);

        String userText = normalizeImageMessage(content);
        long startedAt = System.nanoTime();
        try {
            org.springframework.ai.chat.client.ResponseEntity<ChatResponse, AiChatResult> response = chatClient.prompt(promptFactory.imageMealPrompt(LocalDate.now(clock)))
                    .user(user -> {
                        user.text(userText);
                        images.forEach(image -> user.media(toMimeType(image), image.getResource()));
                    })
                    .call()
                    .responseEntity(AiChatResult.class);

            recordAiUsage(userId, "IMAGE_MEAL", startedAt, true, null, response.response());
            return normalizeAiResult(response.entity());
        } catch (Exception exception) {
            log.warn("Failed to map AI image response to AiChatResult.", exception);
            recordAiUsage(userId, "IMAGE_MEAL", startedAt, false, exception, null);
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

    private MimeType toMimeType(MultipartFile image) {
        return MimeTypeUtils.parseMimeType(image.getContentType());
    }

    private String normalizeImageMessage(String content) {
        if (content == null || content.isBlank()) {
            return DEFAULT_IMAGE_MESSAGE;
        }
        return content.trim();
    }

    private void recordAiUsage(
            Long userId,
            String requestType,
            long startedAt,
            boolean success,
            Exception exception,
            ChatResponse response
    ) {
        try {
            Usage usage = response == null || response.getMetadata() == null
                    ? null
                    : response.getMetadata().getUsage();

            adminMapper.insertAiUsageLog(AiUsageLog.builder()
                    .userId(userId)
                    .requestType(requestType)
                    .model(modelName(response))
                    .latencyMs((System.nanoTime() - startedAt) / 1_000_000)
                    .success(success)
                    .errorMessage(exception == null ? null : truncate(exception.getMessage(), 1000))
                    .inputTokens(toLong(usage == null ? null : usage.getPromptTokens()))
                    .outputTokens(toLong(usage == null ? null : usage.getCompletionTokens()))
                    .totalTokens(toLong(usage == null ? null : usage.getTotalTokens()))
                    .build());
        } catch (Exception loggingException) {
            log.warn("Failed to record AI usage log.", loggingException);
        }
    }

    private String modelName(ChatResponse response) {
        if (response != null && response.getMetadata() != null && response.getMetadata().getModel() != null) {
            return response.getMetadata().getModel();
        }
        return configuredModel;
    }

    private Long toLong(Integer value) {
        return value == null ? null : value.longValue();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
