package com.aihealthcoach.memory.service;

import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.memory.entity.UserMemory;
import com.aihealthcoach.memory.exception.UserMemoryException;
import com.aihealthcoach.memory.mapper.UserMemoryMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserMemoryServiceImpl implements UserMemoryService {

    private static final int MAX_ACTIVE_MEMORY_LIMIT = 10;

    private final UserMemoryMapper userMemoryMapper;

    @Override
    @Transactional
    public UserMemoryResponse createMemory(Long userId, UserMemoryCreateRequest request) {
        UserMemory savedMemory = userMemoryMapper.insertMemory(UserMemory.builder()
                .userId(userId)
                .content(request.content().trim())
                .build());
        return UserMemoryResponse.fromEntity(savedMemory);
    }

    @Override
    @Transactional
    public void deactivateMemory(Long userId, Long memoryId) {
        if (userMemoryMapper.deactivateMemory(userId, memoryId) == 0) {
            throw UserMemoryException.userMemoryNotFound();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserMemoryResponse> findMemoriesByUserId(Long userId) {
        return userMemoryMapper.findMemoriesByUserId(userId)
                .stream()
                .map(UserMemoryResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserMemoryResponse> findActiveMemories(Long userId, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        return userMemoryMapper.findActiveMemories(userId, Math.min(limit, MAX_ACTIVE_MEMORY_LIMIT))
                .stream()
                .map(UserMemoryResponse::fromEntity)
                .toList();
    }
}
