package com.aihealthcoach.chat.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;

import com.aihealthcoach.chat.entity.ChatMessage;

@Mapper
public interface ChatMapper {
    List<ChatMessage> findMessagesByUserId(Long userId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(ChatMessage message);
}
