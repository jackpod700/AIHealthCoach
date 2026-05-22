package com.aihealthcoach.user.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private Long id;
    private String email;
    private String password;
    private String nickname;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
