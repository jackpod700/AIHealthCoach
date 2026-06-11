package com.aihealthcoach.chat.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageSendResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.service.AiChatService;
import com.aihealthcoach.chat.service.AiExerciseProposalService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final AiChatService aiChatService;
    private final AiMealProposalService aiMealProposalService;
    private final AiExerciseProposalService aiExerciseProposalService;
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
        AiChatResult aiResult = aiChatService.generate(message);
        messages.add(chatService.insert(ChatMessage.builder()
                .userId(userId)
                .role("ASSISTANT")
                .content(aiResult.assistantMessage())
                .build()));
        return ResponseEntity.ok(new ChatMessageSendResponse(
                messages,
                aiMealProposalService.createProposal(aiResult.mealExtraction()),
                aiExerciseProposalService.createProposal(aiResult.exerciseExtraction())
        ));
    }

    @PostMapping("/messages/images")
    public ResponseEntity<ChatMessageSendResponse> insertImageMessage(
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            Authentication authentication
    ) {
        List<ChatMessageResponse> messages = new ArrayList<>();
        Long userId = (Long) authentication.getPrincipal();
        String userMessageSummary = imageMessageSummary(content, images);

        messages.add(chatService.insert(ChatMessage.builder()
                .userId(userId)
                .role("USER")
                .content(userMessageSummary)
                .build()));

        AiChatResult aiResult = aiChatService.generateWithImages(content, images);
        messages.add(chatService.insert(ChatMessage.builder()
                .userId(userId)
                .role("ASSISTANT")
                .content(aiResult.assistantMessage())
                .build()));

        return ResponseEntity.ok(new ChatMessageSendResponse(
                messages,
                aiMealProposalService.createProposal(aiResult.mealExtraction()),
                aiExerciseProposalService.createProposal(aiResult.exerciseExtraction())
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

    private String imageMessageSummary(String content, List<MultipartFile> images) {
        int imageCount = images == null ? 0 : images.size();
        String trimmedContent = content == null ? "" : content.trim();
        String prefix = "사진 " + imageCount + "장을 업로드했습니다.";

        if (trimmedContent.isBlank()) {
            return prefix;
        }

        return prefix + "\n" + trimmedContent;
    }
    
}
