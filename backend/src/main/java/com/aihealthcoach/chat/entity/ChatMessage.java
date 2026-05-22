package com.aihealthcoach.chat.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {
    private Long id;
    private Long userId;
    private String role;
    private String content;
    private LocalDateTime createdAt;
}
