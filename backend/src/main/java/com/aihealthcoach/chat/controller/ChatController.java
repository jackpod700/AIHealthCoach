package com.aihealthcoach.chat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.service.ChatService;

import lombok.RequiredArgsConstructor;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessage>> getMethodName(@RequestParam int userId) {
        return ResponseEntity.ok(chatService.findMessagesByUserId(userId));
    }
    
}
