package com.aihealthcoach.chat.service;

import java.util.function.Consumer;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;

public interface AssistantStreamingLlmService {

    void streamAssistantMessage(LlmRequest request, Consumer<String> onDelta);
}
