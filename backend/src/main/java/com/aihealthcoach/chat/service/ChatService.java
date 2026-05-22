package com.aihealthcoach.chat.service;

import java.util.List;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;

public interface ChatService {
    public List<ChatMessageResponse> findMessagesByUserId(Long userId);

    public ChatMessageResponse insert(ChatMessageRequest message);
}
