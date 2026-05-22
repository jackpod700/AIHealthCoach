package com.aihealthcoach.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.entity.ChatMessage;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService{

    private final ChatClient chatClient;
    private final ChatService chatService;

    public ChatMessageResponse generateReply(ChatMessageRequest userMessage) {
        String aiReply = chatClient.prompt("""
                        너는 AI Health Coach 챗봇이다.
                        사용자의 식단 기록, 운동 기록, 건강 습관 관리를 돕는다.
                        의료 진단처럼 단정하지 말고, 일반적인 건강 관리 조언으로 답한다.
                        답변은 한국어로 간결하고 친근하게 작성한다.
                        """)
                            .user(userMessage.content())
                            .call()
                            .content();
        
        return chatService.insert(ChatMessage.builder()
                            .userId(userMessage.id())
                            .content(aiReply)
                            .role("ASSISTANT")
                            .build());
    }
}
