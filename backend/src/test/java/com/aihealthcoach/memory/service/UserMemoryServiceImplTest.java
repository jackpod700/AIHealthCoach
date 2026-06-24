package com.aihealthcoach.memory.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryCreateRequest;
import com.aihealthcoach.memory.dto.UserMemoryDto.UserMemoryResponse;
import com.aihealthcoach.memory.entity.UserMemory;
import com.aihealthcoach.memory.exception.UserMemoryErrorCode;
import com.aihealthcoach.memory.exception.UserMemoryException;
import com.aihealthcoach.memory.mapper.UserMemoryMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserMemoryServiceImplTest {

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @Mock
    private UserMemoryMapper userMemoryMapper;

    private UserMemoryServiceImpl userMemoryService;

    @BeforeEach
    void setUp() {
        userMemoryService = new UserMemoryServiceImpl(userMemoryMapper);
    }

    @Test
    void createMemoryTrimsContentAndPreservesRepeatedRequestsAsSeparateRows() {
        when(userMemoryMapper.insertMemory(any(UserMemory.class))).thenReturn(
                memory(1L, USER_ID, "유제품은 피하고 싶어", true, 1),
                memory(2L, USER_ID, "유제품은 피하고 싶어", true, 2)
        );

        UserMemoryResponse first = userMemoryService.createMemory(
                USER_ID,
                new UserMemoryCreateRequest("  유제품은 피하고 싶어  ")
        );
        UserMemoryResponse second = userMemoryService.createMemory(
                USER_ID,
                new UserMemoryCreateRequest("유제품은 피하고 싶어")
        );

        ArgumentCaptor<UserMemory> captor = ArgumentCaptor.forClass(UserMemory.class);
        verify(userMemoryMapper, org.mockito.Mockito.times(2)).insertMemory(captor.capture());
        assertThat(captor.getAllValues()).extracting(UserMemory::getContent)
                .containsExactly("유제품은 피하고 싶어", "유제품은 피하고 싶어");
        assertThat(first.id()).isEqualTo(1L);
        assertThat(second.id()).isEqualTo(2L);
    }

    @Test
    void deactivateMemoryUsesTheAuthenticatedUserId() {
        when(userMemoryMapper.deactivateMemory(USER_ID, 10L)).thenReturn(1);

        userMemoryService.deactivateMemory(USER_ID, 10L);

        verify(userMemoryMapper).deactivateMemory(USER_ID, 10L);
    }

    @Test
    void deactivateMemoryRejectsMissingOrOtherUsersMemory() {
        when(userMemoryMapper.deactivateMemory(USER_ID, 10L)).thenReturn(0);

        assertThatThrownBy(() -> userMemoryService.deactivateMemory(USER_ID, 10L))
                .isInstanceOf(UserMemoryException.class)
                .extracting("errorCode")
                .isEqualTo(UserMemoryErrorCode.USER_MEMORY_NOT_FOUND);

        verify(userMemoryMapper).deactivateMemory(USER_ID, 10L);
    }

    @Test
    void findMemoriesByUserIdReturnsActiveAndInactiveMemories() {
        when(userMemoryMapper.findMemoriesByUserId(USER_ID)).thenReturn(List.of(
                memory(2L, USER_ID, "비활성 memory", false, 2),
                memory(1L, USER_ID, "활성 memory", true, 1)
        ));

        List<UserMemoryResponse> result = userMemoryService.findMemoriesByUserId(USER_ID);

        assertThat(result).extracting(UserMemoryResponse::active).containsExactly(false, true);
        verify(userMemoryMapper).findMemoriesByUserId(USER_ID);
    }

    @Test
    void findActiveMemoriesReturnsTheNewestTenAtMost() {
        List<UserMemory> memories = List.of(
                memory(12L, USER_ID, "최근 memory", true, 12),
                memory(11L, USER_ID, "이전 memory", true, 11)
        );
        when(userMemoryMapper.findActiveMemories(USER_ID, 10)).thenReturn(memories);

        List<UserMemoryResponse> result = userMemoryService.findActiveMemories(USER_ID, 20);

        assertThat(result).extracting(UserMemoryResponse::content)
                .containsExactly("최근 memory", "이전 memory");
        verify(userMemoryMapper).findActiveMemories(USER_ID, 10);
    }

    @Test
    void findActiveMemoriesReturnsEmptyWithoutQueryForNonPositiveLimit() {
        List<UserMemoryResponse> result = userMemoryService.findActiveMemories(OTHER_USER_ID, 0);

        assertThat(result).isEmpty();
        verifyNoInteractions(userMemoryMapper);
    }

    private UserMemory memory(Long id, Long userId, String content, boolean active, int minute) {
        return UserMemory.builder()
                .id(id)
                .userId(userId)
                .content(content)
                .active(active)
                .createdAt(LocalDateTime.of(2026, 6, 20, 12, minute))
                .updatedAt(LocalDateTime.of(2026, 6, 20, 12, minute))
                .build();
    }
}
