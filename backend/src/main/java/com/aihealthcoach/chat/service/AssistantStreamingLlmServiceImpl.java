package com.aihealthcoach.chat.service;

import java.util.function.Consumer;

import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AssistantStreamingLlmServiceImpl implements AssistantStreamingLlmService {

    private final AiChatClientGateway aiChatClientGateway;

    @Override
    public void streamAssistantMessage(LlmRequest request, Consumer<String> onDelta) {
        aiChatClientGateway.streamTextChat(request.systemPrompt(), request.userMessage())
                .doOnNext(onDelta)
                .blockLast();
    }
}
