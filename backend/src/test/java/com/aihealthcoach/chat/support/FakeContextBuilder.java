package com.aihealthcoach.chat.support;

import java.time.LocalDate;

import com.aihealthcoach.chat.dto.ChatContextDto.UserChatContext;
import com.aihealthcoach.chat.service.ContextBuilder;

public class FakeContextBuilder implements ContextBuilder {

    private UserChatContext context;
    private RuntimeException exception;

    public FakeContextBuilder withContext(UserChatContext context) {
        this.context = context;
        return this;
    }

    public FakeContextBuilder failWith(RuntimeException exception) {
        this.exception = exception;
        return this;
    }

    @Override
    public UserChatContext build(Long userId, LocalDate contextDate) {
        if (exception != null) {
            throw exception;
        }
        return context;
    }
}
