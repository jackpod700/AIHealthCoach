package com.aihealthcoach.chat.service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface AiChatService {
    AiChatResult generate(Long userId, ChatMessageRequest userMessage);

    AiChatResult generateWithImages(Long userId, String content, List<MultipartFile> images);
}
