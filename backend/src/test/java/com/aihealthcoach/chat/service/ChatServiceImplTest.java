package com.aihealthcoach.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.chat.entity.ChatMessage;
import com.aihealthcoach.chat.mapper.ChatMapper;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatMapper chatMapper;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatServiceImpl(chatMapper);
    }

    @Test
    void findRecentMessagesReturnsTurnsInChronologicalOrder() {
        ChatMessage latest = ChatMessage.builder()
                .id(2L)
                .role("ASSISTANT")
                .content("두 번째 메시지")
                .createdAt(LocalDateTime.of(2026, 6, 8, 12, 1))
                .build();
        ChatMessage oldest = ChatMessage.builder()
                .id(1L)
                .role("USER")
                .content("첫 번째 메시지")
                .createdAt(LocalDateTime.of(2026, 6, 8, 12, 0))
                .build();
        when(chatMapper.findRecentMessages(1L, 10)).thenReturn(List.of(latest, oldest));

        List<ChatMessageResponse> messages = chatService.findRecentMessages(1L, 10);

        assertThat(messages).extracting(ChatMessageResponse::content)
                .containsExactly("첫 번째 메시지", "두 번째 메시지");
    }

    @Test
    void findRecentMessagesSkipsMapperForNonPositiveLimit() {
        assertThat(chatService.findRecentMessages(1L, 0)).isEmpty();

        verify(chatMapper, never()).findRecentMessages(1L, 0);
    }
}
