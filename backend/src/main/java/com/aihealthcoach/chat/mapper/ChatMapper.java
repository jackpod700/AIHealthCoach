package com.aihealthcoach.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.chat.entity.ChatMessage;

@Mapper
public interface ChatMapper {
    List<ChatMessage> findMessagesByUserId(Long userId);

    List<ChatMessage> findRecentMessages(@Param("userId") Long userId, @Param("limit") int limit);

    ChatMessage insertMessage(ChatMessage message);
}
