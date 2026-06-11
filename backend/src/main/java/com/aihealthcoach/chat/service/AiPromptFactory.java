package com.aihealthcoach.chat.service;

import java.time.LocalDate;

import org.springframework.stereotype.Component;

@Component
public class AiPromptFactory {

    public String textChatPrompt(LocalDate today) {
        return commonPrompt(today) + """

                mealIntent must be true only when the user describes food they ate or explicitly asks to record food.
                exerciseIntent must be true only when the user describes exercise they did or explicitly asks to record exercise.
                If the user asks for recommendations, nutrition info, greetings, or general chat, use both intents false.
                Quantity is a simple multiplier. If unclear, use null.
                """;
    }

    public String imageMealPrompt(LocalDate today) {
        return commonPrompt(today) + """

                The user may attach one or more images.
                First decide whether the images clearly contain food.
                If no food is visible, return mealIntent false and explain in Korean that food was not found.
                If food is visible but uncertain, do not invent details. Ask the user for more information and return mealIntent false.
                If food is visible, extract likely food names and simple quantity multipliers.
                Use the user's optional text to infer mealDate and mealType when possible.
                Do not claim the meal has been saved. The user must confirm the proposal first.
                """;
    }

    private String commonPrompt(LocalDate today) {
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

                For relative dates, convert them to yyyy-MM-dd using today's date.
                If meal date or exercise date is truly absent, use null.
                If exercise duration is unclear, use null and include "durationMinutes" in missingFields.
                Normalize exercise intensity:
                - LOW for light or easy exercise
                - MEDIUM for normal or moderate exercise
                - HIGH for hard or intense exercise
                """.formatted(today);
    }
}
