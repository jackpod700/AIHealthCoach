package com.aihealthcoach.memory.service;

import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import java.util.List;

public interface UserMemoryService {
    UserMemoryResponse createMemory(Long userId, UserMemoryCreateRequest request);

    void deactivateMemory(Long userId, Long memoryId);

    List<UserMemoryResponse> findMemoriesByUserId(Long userId);

    List<UserMemoryResponse> findActiveMemories(Long userId, int limit);
}
