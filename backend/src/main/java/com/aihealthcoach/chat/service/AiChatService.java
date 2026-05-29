package com.aihealthcoach.chat.service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;

public interface AiChatService {
    public ChatMessageResponse generateReply(Long userId, ChatMessageRequest userMessage);
}
