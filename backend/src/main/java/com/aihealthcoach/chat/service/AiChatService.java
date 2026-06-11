package com.aihealthcoach.chat.service;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface AiChatService {
    AiChatResult generate(ChatMessageRequest userMessage);

    AiChatResult generateWithImages(String content, List<MultipartFile> images);
}
