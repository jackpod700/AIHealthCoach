package com.aihealthcoach.chat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.chat.dto.ChatDto.AiChatRequest;
import com.aihealthcoach.chat.dto.ChatDto.AiChatResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.service.AiChatService;
import com.aihealthcoach.chat.service.ChatService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
    public ResponseEntity<List<ChatMessage>> getMessage(@RequestParam Long userId) {
        return ResponseEntity.ok(chatService.findMessagesByUserId(userId));
    }

    @PostMapping("/messages")
    public ResponseEntity<?> insertMessage(@RequestBody ChatMessage message){
        return ResponseEntity.ok(chatService.insert(message));
    }

    @PostMapping("/ai")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        String aiMessage = aiChatService.generateReply(request.message());

        return ResponseEntity.ok(AiChatResponse.builder()
                .userMessage(request.message())
                .aiMessage(aiMessage)
                .build());
    }
    
}
