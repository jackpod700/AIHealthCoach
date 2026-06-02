package com.aihealthcoach.chat.service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;

public interface AiChatService {
    AiChatResult generate(ChatMessageRequest userMessage);
}
