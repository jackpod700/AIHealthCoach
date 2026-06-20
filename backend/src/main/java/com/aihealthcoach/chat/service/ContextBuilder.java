package com.aihealthcoach.chat.service;

import java.time.LocalDate;

import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;

public interface ContextBuilder {

    UserChatContext build(Long userId, LocalDate contextDate);
}
