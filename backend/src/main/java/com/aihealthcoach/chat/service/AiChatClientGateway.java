package com.aihealthcoach.chat.service;

import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aihealthcoach.admin.aop.AiUsageTracked;
import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatClientGateway {

    private final ChatClient chatClient;

    /**
     * Spring AI 호출을 한 곳에 모아 AOP가 가로챌 수 있는 경계를 만든다.
     * Spring AI ResponseEntity를 그대로 반환해야 aspect가 ChatResponse metadata를 읽을 수 있다.
     */
    @AiUsageTracked(requestType = "TEXT_CHAT")
    public ResponseEntity<ChatResponse, AiChatResult> callTextChat(Long userId, String systemPrompt, String userContent) {
        return chatClient.prompt(systemPrompt)
                .user(userContent)
                .call()
                .responseEntity(AiChatResult.class);
    }

    /**
     * 이미지 분석도 같은 gateway를 통과시켜 텍스트 채팅과 동일한 사용량 기록 흐름을 사용한다.
     */
    @AiUsageTracked(requestType = "IMAGE_MEAL")
    public ResponseEntity<ChatResponse, AiChatResult> callImageMeal(
            Long userId,
            String systemPrompt,
            String userText,
            List<MultipartFile> images
    ) {
        return chatClient.prompt(systemPrompt)
                .user(user -> {
                    user.text(userText);
                    images.forEach(image -> user.media(toMimeType(image), image.getResource()));
                })
                .call()
                .responseEntity(AiChatResult.class);
    }

    private MimeType toMimeType(MultipartFile image) {
        return MimeTypeUtils.parseMimeType(image.getContentType());
    }
}
