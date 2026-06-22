package com.aihealthcoach.chat.service;

import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private final AiChatClientGateway aiChatClientGateway;

    @Override
    public LlmResponse generate(LlmRequest request) {
        ResponseEntity<ChatResponse, String> response;
        if (request.hasImages()) {
            response = aiChatClientGateway.callImageMeal(request);
        } else {
            response = aiChatClientGateway.callTextChat(request.systemPrompt(), request.userMessage());
        }

        return new LlmResponse(response.entity());
    }
}
