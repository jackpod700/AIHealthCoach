package com.aihealthcoach.memory.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserMemory {
    private Long id;
    private Long userId;
    private String content;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
