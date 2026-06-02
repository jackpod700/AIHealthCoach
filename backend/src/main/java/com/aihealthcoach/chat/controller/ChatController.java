package com.aihealthcoach.chat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageSendResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.service.AiChatService;
import com.aihealthcoach.chat.service.ChatMealProposalService;
import com.aihealthcoach.chat.service.ChatService;
import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalRequest;
import com.aihealthcoach.meal.dto.AiMealDto.ConfirmMealProposalResponse;
import com.aihealthcoach.meal.service.AiMealProposalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AiChatService aiChatService;
    private final AiMealProposalService aiMealProposalService;
    private final ChatMealProposalService chatMealProposalService;

    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessageResponse>> getMessage(Authentication authentication) {
        Long userId = (Long)authentication.getPrincipal();
        return ResponseEntity.ok(chatService.findMessagesByUserId(userId));
    }

    @PostMapping("/messages")
    public ResponseEntity<ChatMessageSendResponse> insertMessage(@RequestBody ChatMessageRequest message, Authentication authentication){
        List<ChatMessageResponse> messages = new ArrayList<>();
        Long userId = (Long)authentication.getPrincipal();
        messages.add(chatService.insert(userId, message));
        var aiResult = aiChatService.generate(message);
        messages.add(chatService.insert(ChatMessage.builder()
                .userId(userId)
                .role("ASSISTANT")
                .content(aiResult.assistantMessage())
                .build()));
        return ResponseEntity.ok(new ChatMessageSendResponse(
                messages,
                aiMealProposalService.createProposal(aiResult.mealExtraction())
        ));
    }

    @PostMapping("/meal-proposals/confirm")
    public ResponseEntity<ConfirmMealProposalResponse> confirmMealProposal(
            @Valid @RequestBody ConfirmMealProposalRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(chatMealProposalService.confirm(userId, request));
    }
    
}
