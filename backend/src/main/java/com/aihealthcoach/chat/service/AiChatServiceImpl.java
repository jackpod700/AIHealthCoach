package com.aihealthcoach.chat.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.dto.LlmDto.LlmImage;
import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;
import com.aihealthcoach.chat.exception.ChatException;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.weight.dto.AiWeightDto.ExtractedWeightResult;
import com.aihealthcoach.memory.dto.UserMemoryDto.MemorySaveCommand;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.service.UserMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024L;
    private static final long MAX_TOTAL_IMAGE_SIZE_BYTES = 50L * 1024L;
    private static final int MAX_MEMORY_CONTENT_LENGTH = 500;
    private static final String DEFAULT_IMAGE_MESSAGE = "사진을 분석해서 식단 후보를 만들어줘.";
    private static final String MEMORY_SAVE_SUCCESS_MESSAGE = "요청하신 내용을 기억에 추가했어요.";
    private static final String MEMORY_SAVE_FAILURE_MESSAGE = "기억을 저장하지 못했어요. 다시 한 번 요청해 주세요.";
    private static final String ASSISTANT_MESSAGE_SEPARATOR = "\n\n";

    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final ContextBuilder contextBuilder;
    private final PromptBuilder promptBuilder;
    private final UserMemoryService userMemoryService;

    @Override
    public AiChatResult generate(Long userId, ChatMessageRequest userMessage) {
        try {
            LocalDate contextDate = LocalDate.now(clock);
            UserChatContext context = contextBuilder.build(userId, contextDate);
            LlmResponse response = llmService.generate(promptBuilder.buildText(
                    contextDate,
                    userMessage.content(),
                    context
            ));

            return saveMemoryIfRequested(userId, parseAiResult(response.content()));
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
            LocalDate contextDate = LocalDate.now(clock);
            UserChatContext context = contextBuilder.build(userId, contextDate);
            List<LlmImage> llmImages = images.stream()
                    .map(image -> new LlmImage(toMimeType(image), image.getResource()))
                    .toList();
            LlmResponse response = llmService.generate(promptBuilder.buildImage(
                    contextDate,
                    userText,
                    llmImages,
                    context
            ));

            return saveMemoryIfRequested(userId, parseAiResult(response.content()));
        } catch (Exception exception) {
            log.warn("Failed to map AI image response to AiChatResult.", exception);
            return fallback();
        }
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
                result.exerciseExtraction() == null ? ExtractedExerciseResult.noExercise() : result.exerciseExtraction(),
                result.weightExtraction() == null ? ExtractedWeightResult.noWeight() : result.weightExtraction(),
                normalizeMemorySaveCommand(result.memorySaveCommand())
        );
    }

    private AiChatResult fallback() {
        return new AiChatResult(
                "응답을 정리하지 못했어요. 다시 한 번 자연스럽게 말해 주세요.",
                ExtractedMealResult.noMeal(),
                ExtractedExerciseResult.noExercise(),
                ExtractedWeightResult.noWeight(),
                MemorySaveCommand.noCommand()
        );
    }

    private AiChatResult saveMemoryIfRequested(Long userId, AiChatResult result) {
        MemorySaveCommand command = result.memorySaveCommand();
        if (command == null || !command.memorySaveIntent()) {
            return result;
        }

        if (command.content() == null || command.content().isBlank()
                || command.content().length() > MAX_MEMORY_CONTENT_LENGTH) {
            return appendMemorySaveMessage(result, MEMORY_SAVE_FAILURE_MESSAGE);
        }

        try {
            userMemoryService.createMemory(userId, new UserMemoryCreateRequest(command.content()));
            return appendMemorySaveMessage(result, MEMORY_SAVE_SUCCESS_MESSAGE);
        } catch (Exception exception) {
            log.warn("Failed to save user memory from AI chat.", exception);
            return appendMemorySaveMessage(result, MEMORY_SAVE_FAILURE_MESSAGE);
        }
    }

    private MemorySaveCommand normalizeMemorySaveCommand(MemorySaveCommand command) {
        if (command == null || !command.memorySaveIntent() || command.content() == null || command.content().isBlank()) {
            return MemorySaveCommand.noCommand();
        }

        return new MemorySaveCommand(true, command.content().trim());
    }

    private AiChatResult appendMemorySaveMessage(AiChatResult result, String memorySaveMessage) {
        return new AiChatResult(
                result.assistantMessage().trim() + ASSISTANT_MESSAGE_SEPARATOR + memorySaveMessage,
                result.mealExtraction(),
                result.exerciseExtraction(),
                result.weightExtraction(),
                result.memorySaveCommand()
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
