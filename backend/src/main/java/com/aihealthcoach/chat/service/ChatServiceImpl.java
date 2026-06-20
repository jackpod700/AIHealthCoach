package com.aihealthcoach.chat.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.mapper.ChatMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMapper chatDao;

    @Override
    public List<ChatMessageResponse> findMessagesByUserId(Long userId) {
        List<ChatMessage> messages = chatDao.findMessagesByUserId(userId);
        return messages.stream().map(ChatMessageResponse::fromEntity).toList();
    }

    @Override
    public List<ChatMessageResponse> findRecentMessages(Long userId, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<ChatMessageResponse> messages = new ArrayList<>(chatDao.findRecentMessages(userId, limit)
                .stream()
                .map(ChatMessageResponse::fromEntity)
                .toList());
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public ChatMessageResponse insert(Long userId, ChatMessageRequest message) {
        ChatMessage savedMessage = chatDao.insertMessage(message.toEntity(userId));
        return ChatMessageResponse.fromEntity(savedMessage);
    }

    @Override
    public ChatMessageResponse insert(ChatMessage message) {
        ChatMessage savedMessage = chatDao.insertMessage(message);
        return ChatMessageResponse.fromEntity(savedMessage);
    }

}
