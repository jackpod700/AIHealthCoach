package com.aihealthcoach.chat.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.mapper.ChatMapper;

@Service
public class ChatServiceImpl implements ChatService{

    @Autowired
    private ChatMapper chatDao;

    @Override
    public List<ChatMessageResponse> findMessagesByUserId(Long userId) {
        List<ChatMessage> messages = chatDao.findMessagesByUserId(userId);
        return messages.stream().map(ChatMessageResponse::fromEntity).toList();
    }

    @Override
    public ChatMessageResponse insert(ChatMessageRequest message) {
        ChatMessage savedMessage = chatDao.insertMessage(message.toEntity());
        return ChatMessageResponse.fromEntity(savedMessage);
    }
    
}
