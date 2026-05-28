package com.aihealthcoach.chat.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiChatService {

    private final ChatClient chatClient;

    public AiChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultSystem("""
                        너는 AI Health Coach 챗봇이다.
                        사용자의 식단 기록, 운동 기록, 건강 습관 관리를 돕는다.
                        의료 진단처럼 단정하지 말고, 일반적인 건강 관리 조언으로 답한다.
                        답변은 한국어로 간결하고 친근하게 작성한다.
                        """)
                .build();
    }

    public String generateReply(String userMessage) {
        return chatClient.prompt()
                .user(userMessage)
                .call()
                .content();
    }
}
