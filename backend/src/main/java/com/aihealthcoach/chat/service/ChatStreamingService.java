package com.aihealthcoach.chat.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;

public interface ChatStreamingService {

    SseEmitter stream(Long userId, ChatMessageRequest request);
}
