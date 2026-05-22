package com.aihealthcoach.chat.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.mapper.ChatMapper;

@Service
public class ChatServiceImpl implements ChatService{

    @Autowired
    private ChatMapper chatDao;

    @Override
    public List<ChatMessage> findMessagesByUserId(Long userId) {
        return chatDao.findMessagesByUserId(userId);
    }

    @Override
    public ChatMessage insert(ChatMessage message) {
        chatDao.insert(message);
        return message;
    }
    
}
