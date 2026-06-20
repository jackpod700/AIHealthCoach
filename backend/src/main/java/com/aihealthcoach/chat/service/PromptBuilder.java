package com.aihealthcoach.chat.service;

import java.time.LocalDate;
import java.util.List;

import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.dto.LlmDto.LlmImage;
import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;

public interface PromptBuilder {
    LlmRequest buildText(LocalDate contextDate, String userMessage, UserChatContext context);

    LlmRequest buildImage(
            LocalDate contextDate,
            String userMessage,
            List<LlmImage> images,
            UserChatContext context
    );
}
