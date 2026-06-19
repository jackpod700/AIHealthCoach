package com.aihealthcoach.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LlmServiceImpl implements LlmService {

    private final ChatClient chatClient;

    @Override
    public LlmResponse generate(LlmRequest request) {
        String content;
        if (request.hasImages()) {
            content = chatClient.prompt(request.systemPrompt())
                    .user(user -> {
                        user.text(request.userMessage());
                        request.images().forEach(image -> user.media(image.mimeType(), image.resource()));
                    })
                    .call()
                    .content();
        } else {
            content = chatClient.prompt(request.systemPrompt())
                    .user(request.userMessage())
                    .call()
                    .content();
        }

        return new LlmResponse(content);
    }
}
