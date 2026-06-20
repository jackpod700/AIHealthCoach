package com.aihealthcoach.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.aihealthcoach.chat.dto.ChatDto.AiChatResult;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageRequest;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.dto.ChatDto.ChatMessageSendResponse;
import com.aihealthcoach.chat.service.AiChatService;
import com.aihealthcoach.chat.service.AiExerciseProposalService;
import com.aihealthcoach.chat.service.ChatMealProposalService;
import com.aihealthcoach.chat.service.ChatService;
import com.aihealthcoach.exercise.dto.AiExerciseDto.ExtractedExerciseResult;
import com.aihealthcoach.meal.dto.AiMealDto.ExtractedMealResult;
import com.aihealthcoach.meal.service.AiMealProposalService;
import com.aihealthcoach.weight.dto.AiWeightDto.ExtractedWeightResult;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final Long USER_ID = 1L;

    @Mock
    private ChatService chatService;

    @Mock
    private AiChatService aiChatService;

    @Mock
    private AiMealProposalService aiMealProposalService;

    @Mock
    private AiExerciseProposalService aiExerciseProposalService;

    @Mock
    private ChatMealProposalService chatMealProposalService;

    @Mock
    private Authentication authentication;

    @Test
    void insertMessageBuildsAiResponseBeforePersistingTheCurrentUserMessage() {
        ChatMessageRequest request = new ChatMessageRequest("어제 먹은 내용을 기억해?");
        AiChatResult fallbackResult = new AiChatResult(
                "응답을 정리하지 못했어요. 다시 한 번 자연스럽게 말해 주세요.",
                ExtractedMealResult.noMeal(),
                ExtractedExerciseResult.noExercise(),
                ExtractedWeightResult.noWeight()
        );
        when(authentication.getPrincipal()).thenReturn(USER_ID);
        when(aiChatService.generate(USER_ID, request)).thenReturn(fallbackResult);
        when(chatService.insert(USER_ID, request)).thenReturn(message("USER", request.content()));
        when(chatService.insert(org.mockito.ArgumentMatchers.any())).thenReturn(message("ASSISTANT", fallbackResult.assistantMessage()));
        ChatController controller = new ChatController(
                chatService,
                aiChatService,
                aiMealProposalService,
                aiExerciseProposalService,
                chatMealProposalService
        );

        ResponseEntity<ChatMessageSendResponse> response = controller.insertMessage(request, authentication);

        assertThat(response.getBody().messages()).extracting(ChatMessageResponse::role)
                .containsExactly("USER", "ASSISTANT");
        InOrder inOrder = inOrder(aiChatService, chatService);
        inOrder.verify(aiChatService).generate(USER_ID, request);
        inOrder.verify(chatService).insert(USER_ID, request);
        inOrder.verify(chatService).insert(org.mockito.ArgumentMatchers.any());
    }

    private ChatMessageResponse message(String role, String content) {
        return new ChatMessageResponse(role, content, LocalDateTime.of(2026, 6, 20, 12, 0));
    }
}
