package com.aihealthcoach.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import com.aihealthcoach.admin.aop.AiUsageTracked;
import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AiChatClientGateway {

    private final ChatClient chatClient;

    /**
     * Spring AI 호출을 한 곳에 모아 AOP가 가로챌 수 있는 경계를 만든다.
     * ChatResponse를 그대로 반환해야 aspect가 token usage 등의 metadata를 읽을 수 있다.
     */
    @AiUsageTracked(requestType = "TEXT_CHAT")
    public ChatResponse callTextChat(String systemPrompt, String userContent) {
        return chatClient.prompt(systemPrompt)
                .user(userContent)
                .call()
                .chatResponse();
    }

    public Flux<String> streamTextChat(String systemPrompt, String userContent) {
        return chatClient.prompt(systemPrompt)
                .user(userContent)
                .stream()
                .content();
    }

    /**
     * 이미지 분석도 같은 gateway를 통과시켜 텍스트 채팅과 동일한 사용량 기록 흐름을 사용한다.
     */
    @AiUsageTracked(requestType = "IMAGE_MEAL")
    public ChatResponse callImageMeal(LlmRequest request) {
        return chatClient.prompt(request.systemPrompt())
                .user(user -> {
                    user.text(request.userMessage());
                    request.images().forEach(image -> user.media(image.mimeType(), image.resource()));
                })
                .call()
                .chatResponse();
    }
}
