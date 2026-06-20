package com.aihealthcoach.chat.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aihealthcoach.chat.dto.LlmDto.LlmRequest;
import com.aihealthcoach.chat.dto.LlmDto.LlmResponse;
import com.aihealthcoach.chat.service.LlmService;

public class FakeLlmService implements LlmService {

    private final Map<String, LlmResponse> responsesByUserMessage = new HashMap<>();
    private final List<LlmRequest> requests = new ArrayList<>();
    private final List<String> missingResponseMessages = new ArrayList<>();

    public FakeLlmService respondTo(String userMessage, String responseContent) {
        responsesByUserMessage.put(userMessage, new LlmResponse(responseContent));
        return this;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        requests.add(request);

        LlmResponse response = responsesByUserMessage.get(request.userMessage());
        if (response == null) {
            missingResponseMessages.add(request.userMessage());
            return new LlmResponse("");
        }

        return response;
    }

    public LlmRequest lastRequest() {
        if (requests.isEmpty()) {
            throw new AssertionError("FakeLlmService did not receive a request.");
        }
        return requests.getLast();
    }

    public void assertAllRequestsMatched() {
        if (!missingResponseMessages.isEmpty()) {
            throw new AssertionError("No fake LLM response configured for: " + missingResponseMessages);
        }
    }
}
