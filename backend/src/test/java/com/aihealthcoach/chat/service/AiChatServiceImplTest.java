package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.exception.ChatException;
import com.aihealthcoach.chat.support.FakeContextBuilder;
import com.aihealthcoach.chat.support.FakeLlmService;
import com.aihealthcoach.chat.support.FakeUserMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiChatServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-08T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void generateUsesInjectedLlmServiceResponse() {
        String userMessage = "아침에 오트밀 먹었어";
        FakeLlmService llmService = new FakeLlmService().respondTo(userMessage, """
                {
                  "assistantMessage": "기록할 식사를 찾았어요.",
                  "mealExtraction": {
                    "mealIntent": true,
                    "mealDate": "2026-06-08",
                    "mealType": "BREAKFAST",
                    "items": [
                      {"name": "oatmeal", "quantity": 1}
                    ]
                  }
                }
                """);
        AiChatServiceImpl service = newService(llmService);

        AiChatResult result = service.generate(1L, new ChatMessageRequest(userMessage));

        assertThat(result.assistantMessage()).isEqualTo("기록할 식사를 찾았어요.");
        assertThat(result.mealExtraction().mealIntent()).isTrue();
        assertThat(result.mealExtraction().mealDate()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(result.mealExtraction().items()).extracting("name").containsExactly("oatmeal");
        assertThat(llmService.lastRequest().stableSystemPrompt()).contains("You are AI Health Coach.");
        assertThat(llmService.lastRequest().dynamicContextPrompt()).contains("<reference_date>\n2026-06-08");
        assertThat(llmService.lastRequest().userMessage()).isEqualTo(userMessage);
        assertThat(llmService.lastRequest().images()).isEmpty();
    }

    @Test
    void generateReturnsFallbackWhenContextBuildingFails() {
        FakeLlmService llmService = new FakeLlmService();
        AiChatServiceImpl service = newService(
                llmService,
                new FakeContextBuilder().failWith(new IllegalStateException("context unavailable"))
        );

        AiChatResult result = service.generate(1L, new ChatMessageRequest("아침에 오트밀 먹었어"));

        assertThat(result.assistantMessage()).isNotBlank();
        assertThat(result.mealExtraction().mealIntent()).isFalse();
        assertThat(llmService.requestCount()).isZero();
    }

    @Test
    void generateWithImagesUsesInjectedLlmServiceResponse() {
        FakeLlmService llmService = new FakeLlmService().respondTo("점심 사진이야", """
                {
                  "assistantMessage": "사진에서 식사를 찾았어요.",
                  "mealExtraction": {
                    "mealIntent": true,
                    "mealDate": "2026-06-08",
                    "mealType": "LUNCH",
                    "items": [
                      {"name": "salad", "quantity": 1}
                    ]
                  }
                }
                """);
        AiChatServiceImpl service = newService(llmService);
        MockMultipartFile image = new MockMultipartFile(
                "images",
                "meal.jpg",
                "image/jpeg",
                "image".getBytes()
        );

        AiChatResult result = service.generateWithImages(1L, "점심 사진이야", List.of(image));

        assertThat(result.assistantMessage()).isEqualTo("사진에서 식사를 찾았어요.");
        assertThat(result.mealExtraction().mealIntent()).isTrue();
        assertThat(llmService.lastRequest().stableSystemPrompt()).contains("If no food is visible");
        assertThat(llmService.lastRequest().userMessage()).isEqualTo("점심 사진이야");
        assertThat(llmService.lastRequest().images()).hasSize(1);
        assertThat(llmService.lastRequest().images().getFirst().mimeType().toString()).isEqualTo("image/jpeg");
    }

    @Test
    void parseAiResultReadsAssistantMessageAndMealExtraction() {
        AiChatServiceImpl service = newService();
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
                  },
                  "weightExtraction": {
                    "weightIntent": true,
                    "recordDate": "2026-06-02",
                    "weightKg": 68.4
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
        assertThat(result.weightExtraction().weightIntent()).isTrue();
        assertThat(result.weightExtraction().recordDate()).isEqualTo(LocalDate.of(2026, 6, 2));
        assertThat(result.weightExtraction().weightKg()).isEqualByComparingTo(new BigDecimal("68.4"));
        assertThat(result.memorySaveCommand().memorySaveIntent()).isFalse();
    }

    @Test
    void generateSavesOnlyAnExplicitMemoryCommand() {
        String userMessage = "유제품은 피하고 싶은 걸 기억해줘";
        FakeLlmService llmService = new FakeLlmService().respondTo(userMessage, """
                {
                  "assistantMessage": "점심으로 라면을 드셨군요! 식사 후보를 만들었어요.",
                  "memorySaveCommand": {
                    "memorySaveIntent": true,
                    "content": "유제품은 피하고 싶어"
                  }
                }
                """);
        FakeUserMemoryService userMemoryService = new FakeUserMemoryService();
        AiChatServiceImpl service = newService(llmService, new FakeContextBuilder(), userMemoryService);

        AiChatResult result = service.generate(1L, new ChatMessageRequest(userMessage));

        assertThat(result.assistantMessage()).isEqualTo(
                "점심으로 라면을 드셨군요! 식사 후보를 만들었어요.\n\n요청하신 내용을 기억에 추가했어요."
        );
        assertThat(result.memorySaveCommand().memorySaveIntent()).isTrue();
        assertThat(userMemoryService.createRequestCount()).isEqualTo(1);
        assertThat(userMemoryService.lastCreateRequest().content()).isEqualTo("유제품은 피하고 싶어");
    }

    @Test
    void generateDoesNotSaveMemoryForGeneralChat() {
        String userMessage = "유제품을 먹지 않는 식단을 추천해줘";
        FakeLlmService llmService = new FakeLlmService().respondTo(userMessage, """
                {
                  "assistantMessage": "유제품 없이도 단백질을 챙길 수 있는 식단을 추천해 드릴게요."
                }
                """);
        FakeUserMemoryService userMemoryService = new FakeUserMemoryService();
        AiChatServiceImpl service = newService(llmService, new FakeContextBuilder(), userMemoryService);

        service.generate(1L, new ChatMessageRequest(userMessage));

        assertThat(userMemoryService.createRequestCount()).isZero();
    }

    @Test
    void generateReturnsFailureMessageWhenMemorySaveFails() {
        String userMessage = "유제품은 피하고 싶은 걸 기억해줘";
        FakeLlmService llmService = new FakeLlmService().respondTo(userMessage, """
                {
                  "assistantMessage": "점심으로 라면을 드셨군요! 식사 후보를 만들었어요.",
                  "memorySaveCommand": {
                    "memorySaveIntent": true,
                    "content": "유제품은 피하고 싶어"
                  }
                }
                """);
        FakeUserMemoryService userMemoryService = new FakeUserMemoryService()
                .failCreateWith(new IllegalStateException("database unavailable"));
        AiChatServiceImpl service = newService(llmService, new FakeContextBuilder(), userMemoryService);

        AiChatResult result = service.generate(1L, new ChatMessageRequest(userMessage));

        assertThat(result.assistantMessage()).isEqualTo(
                "점심으로 라면을 드셨군요! 식사 후보를 만들었어요.\n\n기억을 저장하지 못했어요. 다시 한 번 요청해 주세요."
        );
        assertThat(result.memorySaveCommand().memorySaveIntent()).isTrue();
        assertThat(userMemoryService.createRequestCount()).isEqualTo(1);
    }

    @Test
    void parseAiResultFallsBackWhenJsonIsInvalid() {
        AiChatServiceImpl service = newService();

        AiChatResult result = service.parseAiResult("not json");

        assertThat(result.assistantMessage()).isNotBlank();
        assertThat(result.mealExtraction().mealIntent()).isFalse();
        assertThat(result.mealExtraction().items()).isEmpty();
        assertThat(result.exerciseExtraction().exerciseIntent()).isFalse();
        assertThat(result.weightExtraction().weightIntent()).isFalse();
    }

    @Test
    void textPromptKeepsDateInDynamicContext() {
        PromptBuilder promptBuilder = new PromptBuilderImpl(new AiPromptFactory());

        LlmRequest request = promptBuilder.buildText(
                LocalDate.of(2026, 6, 8),
                "안녕하세요",
                null
        );

        assertThat(request.stableSystemPrompt()).contains("For relative dates");
        assertThat(request.stableSystemPrompt()).contains("do not claim that the memory was saved");
        assertThat(request.stableSystemPrompt()).doesNotContain("2026-06-08");
        assertThat(request.dynamicContextPrompt()).contains("<reference_date>\n2026-06-08");
    }

    @Test
    void generateWithImagesRejectsUnsupportedImageType() {
        AiChatServiceImpl service = newService();
        MockMultipartFile textFile = new MockMultipartFile(
                "images",
                "memo.txt",
                "text/plain",
                "not image".getBytes()
        );

        assertThatThrownBy(() -> service.generateWithImages(1L, "", List.of(textFile)))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void generateWithImagesRejectsImageOverGmsLimit() {
        AiChatServiceImpl service = newService();
        MockMultipartFile largeImage = new MockMultipartFile(
                "images",
                "meal.jpg",
                "image/jpeg",
                new byte[(10 * 1024) + 1]
        );

        assertThatThrownBy(() -> service.generateWithImages(1L, "", List.of(largeImage)))
                .isInstanceOf(ChatException.class);
    }

    @Test
    void imagePromptIncludesNonFoodImageRules() {
        AiPromptFactory promptFactory = new AiPromptFactory();

        String prompt = promptFactory.imageMealPrompt();

        assertThat(prompt).contains("If no food is visible");
        assertThat(prompt).contains("mealIntent false");
    }

    private AiChatServiceImpl newService() {
        return newService(new FakeLlmService(), new FakeContextBuilder(), new FakeUserMemoryService());
    }

    private AiChatServiceImpl newService(LlmService llmService) {
        return newService(llmService, new FakeContextBuilder(), new FakeUserMemoryService());
    }

    private AiChatServiceImpl newService(LlmService llmService, ContextBuilder contextBuilder) {
        return newService(llmService, contextBuilder, new FakeUserMemoryService());
    }

    private AiChatServiceImpl newService(
            LlmService llmService,
            ContextBuilder contextBuilder,
            FakeUserMemoryService userMemoryService
    ) {
        return new AiChatServiceImpl(
                llmService,
                new ObjectMapper(),
                CLOCK,
                contextBuilder,
                new PromptBuilderImpl(new AiPromptFactory()),
                userMemoryService
        );
    }
}
