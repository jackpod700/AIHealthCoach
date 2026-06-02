package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AiChatServiceImplTest {

    @Test
    void parseAiResultReadsAssistantMessageAndMealExtraction() {
        AiChatServiceImpl service = new AiChatServiceImpl(null, new ObjectMapper());
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
                  }
                }
                """;

        var result = service.parseAiResult(json);

        assertThat(result.assistantMessage()).isEqualTo("I found a meal proposal.");
        assertThat(result.mealExtraction().mealIntent()).isTrue();
        assertThat(result.mealExtraction().mealDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(result.mealExtraction().mealType()).isEqualTo("LUNCH");
        assertThat(result.mealExtraction().items()).hasSize(2);
        assertThat(result.mealExtraction().items().get(1).quantity()).isEqualByComparingTo(new BigDecimal("2"));
    }

    @Test
    void parseAiResultFallsBackWhenJsonIsInvalid() {
        AiChatServiceImpl service = new AiChatServiceImpl(null, new ObjectMapper());

        var result = service.parseAiResult("not json");

        assertThat(result.assistantMessage()).isNotBlank();
        assertThat(result.mealExtraction().mealIntent()).isFalse();
        assertThat(result.mealExtraction().items()).isEmpty();
    }
}
