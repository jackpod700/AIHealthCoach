package com.aihealthcoach.chat.support;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.service.AiChatServiceImpl;
import com.aihealthcoach.chat.service.AiPromptFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AiChatHarness {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-06-08T03:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final FakeLlmService fakeLlmService = new FakeLlmService();
    private final AiChatServiceImpl aiChatService = new AiChatServiceImpl(
            fakeLlmService,
            new ObjectMapper(),
            CLOCK,
            new AiPromptFactory()
    );

    public AiChatHarness respondTo(String userMessage, String responseContent) {
        fakeLlmService.respondTo(userMessage, responseContent);
        return this;
    }

    public AiChatResult send(String userMessage) {
        AiChatResult result = aiChatService.generate(new ChatMessageRequest(userMessage));
        fakeLlmService.assertAllRequestsMatched();
        return result;
    }

    public FakeLlmService fakeLlmService() {
        return fakeLlmService;
    }
}
