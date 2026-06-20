package com.aihealthcoach.chat.support;

import java.util.ArrayList;
import java.util.List;

import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.memory.service.UserMemoryService;

public class FakeUserMemoryService implements UserMemoryService {

    private final List<UserMemoryCreateRequest> createRequests = new ArrayList<>();
    private RuntimeException createException;

    public FakeUserMemoryService failCreateWith(RuntimeException exception) {
        this.createException = exception;
        return this;
    }

    @Override
    public UserMemoryResponse createMemory(Long userId, UserMemoryCreateRequest request) {
        createRequests.add(request);
        if (createException != null) {
            throw createException;
        }

        return new UserMemoryResponse((long) createRequests.size(), request.content(), true, null, null);
    }

    @Override
    public void deactivateMemory(Long userId, Long memoryId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<UserMemoryResponse> findMemoriesByUserId(Long userId) {
        return List.of();
    }

    @Override
    public List<UserMemoryResponse> findActiveMemories(Long userId, int limit) {
        return List.of();
    }

    public int createRequestCount() {
        return createRequests.size();
    }

    public UserMemoryCreateRequest lastCreateRequest() {
        if (createRequests.isEmpty()) {
            throw new AssertionError("FakeUserMemoryService did not receive a create request.");
        }
        return createRequests.getLast();
    }
}
