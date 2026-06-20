package com.aihealthcoach.memory.dto;

import com.aihealthcoach.memory.entity.UserMemory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class UserMemoryDto {

    public record UserMemoryCreateRequest(
            @NotBlank(message = "메모리 내용은 빈 칸일 수 없습니다.")
            @Size(max = 500, message = "메모리 내용은 500자 이하여야 합니다.")
            String content
    ) {
    }

    public record UserMemoryResponse(
            Long id,
            String content,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static UserMemoryResponse fromEntity(UserMemory userMemory) {
            return new UserMemoryResponse(
                    userMemory.getId(),
                    userMemory.getContent(),
                    userMemory.isActive(),
                    userMemory.getCreatedAt(),
                    userMemory.getUpdatedAt()
            );
        }
    }

    public record UserMemoryDeleteResponse(
            String message
    ) {
        private static final String DEFAULT_MESSAGE = "해당 사용자 메모리가 비활성화되었습니다.";

        public UserMemoryDeleteResponse {
            if (message == null) {
                message = DEFAULT_MESSAGE;
            }
        }
    }
}
