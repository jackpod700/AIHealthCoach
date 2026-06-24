package com.aihealthcoach.chat.service;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;

public interface LlmService {
    LlmResponse generate(LlmRequest request);
}
