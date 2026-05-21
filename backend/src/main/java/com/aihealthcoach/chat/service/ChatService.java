package com.aihealthcoach.chat.service;

import java.util.List;

import com.aihealthcoach.chat.entity.ChatMessage;

public interface ChatService {
    public List<ChatMessage> findMessagesByUserId(int userId);
}
