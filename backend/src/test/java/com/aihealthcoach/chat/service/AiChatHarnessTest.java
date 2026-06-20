package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.support.AiChatHarness;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;

class AiChatHarnessTest {

    @Test
    void mealScenarioConvertsFakeLlmResponseToAiChatResult() {
        String userMessage = "점심에 김치찌개와 밥을 먹었어";
        AiChatHarness harness = new AiChatHarness().respondTo(userMessage, """
                {
                  "assistantMessage": "점심 식사를 기록할 수 있도록 후보를 찾았어요.",
                  "mealExtraction": {
                    "mealIntent": true,
                    "mealDate": "2026-06-08",
                    "mealType": "LUNCH",
                    "items": [
                      {"name": "김치찌개", "quantity": 1},
                      {"name": "밥", "quantity": 1}
                    ]
                  }
                }
                """);

        AiChatResult result = harness.send(userMessage);

        assertThat(result.assistantMessage()).isEqualTo("점심 식사를 기록할 수 있도록 후보를 찾았어요.");
        assertThat(result.mealExtraction().mealIntent()).isTrue();
        assertThat(result.mealExtraction().mealDate()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(result.mealExtraction().items()).extracting("name").containsExactly("김치찌개", "밥");
        assertThat(harness.fakeLlmService().lastRequest().userMessage()).isEqualTo(userMessage);
    }

    @Test
    void exerciseScenarioConvertsFakeLlmResponseToAiChatResult() {
        String userMessage = "저녁에 30분 케틀벨 운동했어";
        AiChatHarness harness = new AiChatHarness().respondTo(userMessage, """
                {
                  "assistantMessage": "운동 기록 후보를 찾았어요.",
                  "exerciseExtraction": {
                    "exerciseIntent": true,
                    "activityKeyword": "kettlebell",
                    "intensityLevel": "HIGH",
                    "exerciseDate": "2026-06-08",
                    "durationMinutes": 30,
                    "memo": "저녁 운동",
                    "confidence": 0.9,
                    "missingFields": []
                  }
                }
                """);

        AiChatResult result = harness.send(userMessage);

        assertThat(result.assistantMessage()).isEqualTo("운동 기록 후보를 찾았어요.");
        assertThat(result.exerciseExtraction().exerciseIntent()).isTrue();
        assertThat(result.exerciseExtraction().activityKeyword()).isEqualTo("kettlebell");
        assertThat(result.exerciseExtraction().durationMinutes()).isEqualTo(30);
        assertThat(result.exerciseExtraction().confidence()).isEqualByComparingTo(new BigDecimal("0.9"));
        assertThat(result.weightExtraction().weightIntent()).isFalse();
    }

    @Test
    void invalidJsonScenarioReturnsFallbackResult() {
        String userMessage = "오늘은 기록할 내용이 없어";
        AiChatHarness harness = new AiChatHarness().respondTo(userMessage, "not json");

        AiChatResult result = harness.send(userMessage);

        assertThat(result.assistantMessage()).isNotBlank();
        assertThat(result.mealExtraction().mealIntent()).isFalse();
        assertThat(result.exerciseExtraction().exerciseIntent()).isFalse();
        assertThat(result.weightExtraction().weightIntent()).isFalse();
    }

    @Test
    void missingScenarioResponseFailsFast() {
        AiChatHarness harness = new AiChatHarness();

        assertThatThrownBy(() -> harness.send("등록되지 않은 발화"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("No fake LLM response configured");
    }

    @Test
    void scenarioRendersStoredMemoryIntoTheNextLlmRequest() {
        String userMessage = "어제 먹은 내용을 기억해?";
        UserChatContext context = new UserChatContext(
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(new UserMemoryResponse(1L, "유제품은 피하고 싶어", true, null, null))
        );
        AiChatHarness harness = new AiChatHarness()
                .withContext(context)
                .respondTo(userMessage, """
                        {
                          "assistantMessage": "네, 최근 대화를 확인했어요."
                        }
                        """);

        harness.send(userMessage);

        assertThat(harness.fakeLlmService().lastRequest().dynamicContextPrompt())
                .contains("<user_memories>\n- 유제품은 피하고 싶어\n</user_memories>");
    }

    @Test
    void memorySaveScenarioRoutesTheExtractedContentToTheFakeService() {
        String userMessage = "아침 운동을 좋아한다는 걸 기억해줘";
        AiChatHarness harness = new AiChatHarness().respondTo(userMessage, """
                {
                  "assistantMessage": "아침 운동 선호를 반영할게요.",
                  "memorySaveCommand": {
                    "memorySaveIntent": true,
                    "content": "아침 운동을 좋아한다"
                  }
                }
                """);

        AiChatResult result = harness.send(userMessage);

        assertThat(result.assistantMessage()).isEqualTo(
                "아침 운동 선호를 반영할게요.\n\n요청하신 내용을 기억에 추가했어요."
        );
        assertThat(harness.fakeUserMemoryService().lastCreateRequest().content()).isEqualTo("아침 운동을 좋아한다");
    }
}
