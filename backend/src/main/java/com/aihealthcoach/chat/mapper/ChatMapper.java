package com.aihealthcoach.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.aihealthcoach.chat.entity.ChatMessage;

@Mapper
public interface ChatMapper {
    List<ChatMessage> findMessagesByUserId(Long userId);

    ChatMessage insertMessage(ChatMessage message);
}
