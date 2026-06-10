package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiChatServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-08T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void parseAiResultReadsAssistantMessageAndMealExtraction() {
        AiChatServiceImpl service = new AiChatServiceImpl(null, new ObjectMapper(), CLOCK);
        String json = """
                {
                  "assistantMessage": "I found a meal proposal.",
                  "mealExtraction": {
                    "mealIntent": true,
                    "mealDate": "2026-06-02",
                    "mealType": "LUNCH",
                    "items": [
                      {"name": "kimchi stew", "quantity": 1},
                          {"name": "rice", "quantity": 2}
                        ]
                      },
                      "exerciseExtraction": {
                        "exerciseIntent": true,
                        "activityKeyword": "kettlebell",
                        "intensityLevel": "HIGH",
                        "exerciseDate": "2026-06-02",
                        "durationMinutes": 30,
                        "memo": "after work",
                        "confidence": 0.9,
                        "missingFields": []
                      }
                    }
                """;

        AiChatResult result = service.parseAiResult(json);

        assertThat(result.assistantMessage()).isEqualTo("I found a meal proposal.");
        assertThat(result.mealExtraction().mealIntent()).isTrue();
        assertThat(result.mealExtraction().mealDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(result.mealExtraction().mealType()).isEqualTo("LUNCH");
        assertThat(result.mealExtraction().items()).hasSize(2);
        assertThat(result.mealExtraction().items().get(1).quantity()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(result.exerciseExtraction().exerciseIntent()).isTrue();
        assertThat(result.exerciseExtraction().activityKeyword()).isEqualTo("kettlebell");
        assertThat(result.exerciseExtraction().intensityLevel()).isEqualTo("HIGH");
        assertThat(result.exerciseExtraction().durationMinutes()).isEqualTo(30);
    }

    @Test
    void parseAiResultFallsBackWhenJsonIsInvalid() {
        AiChatServiceImpl service = new AiChatServiceImpl(null, new ObjectMapper(), CLOCK);

        AiChatResult result = service.parseAiResult("not json");

        assertThat(result.assistantMessage()).isNotBlank();
        assertThat(result.mealExtraction().mealIntent()).isFalse();
        assertThat(result.mealExtraction().items()).isEmpty();
        assertThat(result.exerciseExtraction().exerciseIntent()).isFalse();
    }

    @Test
    void systemPromptIncludesTodayAndRelativeExerciseDateRules() {
        AiChatServiceImpl service = new AiChatServiceImpl(null, new ObjectMapper(), CLOCK);

        String prompt = service.systemPrompt(LocalDate.of(2026, 6, 8));

        assertThat(prompt).contains("Today's date is 2026-06-08");
        assertThat(prompt).contains("오늘, 방금, 아까");
        assertThat(prompt).contains("today's date");
    }
}
