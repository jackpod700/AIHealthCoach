package com.aihealthcoach.chat.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Override
    public AiChatResult generate(ChatMessageRequest userMessage) {
        String content = chatClient.prompt(systemPrompt(LocalDate.now(clock)))
                .user(userMessage.content())
                .call()
                .content();

        return parseAiResult(content);
    }

    String systemPrompt(LocalDate today) {
        return """
                        You are AI Health Coach.
                        Respond in Korean in a friendly, concise health-coach tone.
                        Do not diagnose like a medical professional.
                        Today's date is %s in Asia/Seoul.

                        Return only JSON with this exact shape:
                        {
                          "assistantMessage": "message to show user",
                          "mealExtraction": {
                            "mealIntent": true,
                            "mealDate": "yyyy-MM-dd or null",
                            "mealType": "BREAKFAST|LUNCH|DINNER|SNACK or null",
                            "items": [{"name": "food name", "quantity": 1}]
                          },
                          "exerciseExtraction": {
                            "exerciseIntent": true,
                            "activityKeyword": "exercise keyword or null",
                            "intensityLevel": "LOW|MEDIUM|HIGH or null",
                            "exerciseDate": "yyyy-MM-dd or null",
                            "durationMinutes": 30,
                            "memo": "short memo or null",
                            "confidence": 0.0,
                            "missingFields": ["exerciseDate", "durationMinutes"]
                          }
                        }

                        mealIntent must be true only when the user describes food they ate or explicitly asks to record food.
                        exerciseIntent must be true only when the user describes exercise they did or explicitly asks to record exercise.
                        If the user asks for recommendations, nutrition info, greetings, or general chat, use both intents false.
                        Quantity is a simple multiplier. If unclear, use null.
                        Normalize exercise intensity:
                        - LOW for light/easy/gently/가볍게/살살
                        - MEDIUM for normal/moderate/보통
                        - HIGH for hard/intense/빡세게/강하게
                        For relative exercise dates, convert them to yyyy-MM-dd using today's date:
                        - 오늘, 방금, 아까, 조금 전, just now, earlier today => today's date
                        - 어제, yesterday => one day before today's date
                        - 그제, two days ago => two days before today's date
                        If exercise date is truly absent, use null and include "exerciseDate" in missingFields.
                        If duration is unclear, use null and include "durationMinutes" in missingFields.
                        """.formatted(today);
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
}
