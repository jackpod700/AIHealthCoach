package com.aihealthcoach.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Override
    public AiChatResult generate(ChatMessageRequest userMessage) {
        String content = chatClient.prompt("""
                        You are AI Health Coach.
                        Respond in Korean in a friendly, concise health-coach tone.
                        Do not diagnose like a medical professional.

                        Return only JSON with this exact shape:
                        {
                          "assistantMessage": "message to show user",
                          "mealExtraction": {
                            "mealIntent": true,
                            "mealDate": "yyyy-MM-dd or null",
                            "mealType": "BREAKFAST|LUNCH|DINNER|SNACK or null",
                            "items": [{"name": "food name", "quantity": 1}]
                          }
                        }

                        mealIntent must be true only when the user describes food they ate or explicitly asks to record food.
                        If the user asks for recommendations, nutrition info, exercise, greetings, or general chat, use mealIntent false and items [].
                        Quantity is a simple multiplier. If unclear, use null.
                        """)
                .user(userMessage.content())
                .call()
                .content();

        return parseAiResult(content);
    }

    AiChatResult parseAiResult(String content) {
        try {
            objectMapper.findAndRegisterModules();
            AiChatResult result = objectMapper.readValue(content, AiChatResult.class);
            if (result.assistantMessage() == null || result.assistantMessage().isBlank()) {
                return fallback();
            }
            if (result.mealExtraction() == null) {
                return new AiChatResult(result.assistantMessage(), ExtractedMealResult.noMeal());
            }
            return result;
        } catch (Exception exception) {
            return fallback();
        }
    }

    private AiChatResult fallback() {
        return new AiChatResult(
                "응답을 정리하지 못했어요. 다시 한 번 자연스럽게 말해 주세요.",
                ExtractedMealResult.noMeal()
        );
    }
}
