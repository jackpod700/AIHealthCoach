package com.aihealthcoach.chat.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.mapper.ChatMapper;
import com.aihealthcoach.summary.entity.DailyChatSummaryChangeSource;
import com.aihealthcoach.summary.service.DailyChatSummaryStateService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMapper chatDao;
    private final DailyChatSummaryStateService dailyChatSummaryStateService;

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
    @Transactional
    public ChatMessageResponse insert(Long userId, ChatMessageRequest message) {
        ChatMessage savedMessage = chatDao.insertMessage(message.toEntity(userId));
        dailyChatSummaryStateService.markChanged(userId, savedMessage.getCreatedAt().toLocalDate(), DailyChatSummaryChangeSource.CHAT);
        return ChatMessageResponse.fromEntity(savedMessage);
    }

    @Override
    @Transactional
    public ChatMessageResponse insert(ChatMessage message) {
        ChatMessage savedMessage = chatDao.insertMessage(message);
        dailyChatSummaryStateService.markChanged(
                savedMessage.getUserId(),
                savedMessage.getCreatedAt().toLocalDate(),
                DailyChatSummaryChangeSource.CHAT
        );
        return ChatMessageResponse.fromEntity(savedMessage);
    }

}
