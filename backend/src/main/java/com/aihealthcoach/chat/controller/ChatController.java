package com.aihealthcoach.chat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.chat.dto.ChatDto.AiChatRequest;
import com.aihealthcoach.chat.dto.ChatDto.AiChatResponse;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.service.AiChatService;
import com.aihealthcoach.chat.service.ChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AiChatService aiChatService;

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessage(Authentication authentication) {
        Long userId = (Long)authentication.getPrincipal();
        return ResponseEntity.ok(chatService.findMessagesByUserId(userId));
    }

    @PostMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> insertMessage(@RequestBody ChatMessageRequest message, Authentication authentication){
        List<ChatMessageResponse> messages = new ArrayList<>();
        Long userId = (Long)authentication.getPrincipal();
        messages.add(chatService.insert(userId, message));
        messages.add(aiChatService.generateReply(userId, message));
        
        return ResponseEntity.ok(messages);
    }
    
}
