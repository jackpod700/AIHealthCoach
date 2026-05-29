package com.aihealthcoach.chat.service;

import java.util.List;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.entity.ChatMessage;

public interface ChatService {
    public List<ChatMessageResponse> findMessagesByUserId(Long userId);

    public ChatMessageResponse insert(Long userId, ChatMessageRequest message);

    public ChatMessageResponse insert(ChatMessage message);
}
