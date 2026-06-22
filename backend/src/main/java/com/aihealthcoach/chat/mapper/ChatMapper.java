package com.aihealthcoach.chat.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.aihealthcoach.chat.entity.ChatMessage;

@Mapper
public interface ChatMapper {
    List<ChatMessage> findMessagesByUserId(Long userId);

    List<ChatMessage> findRecentMessages(@Param("userId") Long userId, @Param("limit") int limit);

    List<ChatMessage> findMessagesByCreatedAtBetween(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    ChatMessage insertMessage(ChatMessage message);
}
