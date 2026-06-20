package com.aihealthcoach.chat.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.service.AiChatServiceImpl;
import com.aihealthcoach.chat.service.AiPromptFactory;
import com.aihealthcoach.chat.service.PromptBuilderImpl;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AiChatHarness {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-08T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final FakeLlmService fakeLlmService = new FakeLlmService();
    private final FakeContextBuilder fakeContextBuilder = new FakeContextBuilder();
    private final FakeUserMemoryService fakeUserMemoryService = new FakeUserMemoryService();
    private final AiChatServiceImpl aiChatService = new AiChatServiceImpl(
            fakeLlmService,
            new ObjectMapper(),
            CLOCK,
            fakeContextBuilder,
            new PromptBuilderImpl(new AiPromptFactory()),
            fakeUserMemoryService
    );

    public AiChatHarness respondTo(String userMessage, String responseContent) {
        fakeLlmService.respondTo(userMessage, responseContent);
        return this;
    }

    public AiChatHarness withContext(UserChatContext context) {
        fakeContextBuilder.withContext(context);
        return this;
    }

    public AiChatResult send(String userMessage) {
        AiChatResult result = aiChatService.generate(1L, new ChatMessageRequest(userMessage));
        fakeLlmService.assertAllRequestsMatched();
        return result;
    }

    public FakeLlmService fakeLlmService() {
        return fakeLlmService;
    }

    public FakeContextBuilder fakeContextBuilder() {
        return fakeContextBuilder;
    }

    public FakeUserMemoryService fakeUserMemoryService() {
        return fakeUserMemoryService;
    }
}
