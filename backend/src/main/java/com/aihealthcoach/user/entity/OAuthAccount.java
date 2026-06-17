package com.aihealthcoach.user.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthAccount {

    private Long id;
    private Long userId;
    private OAuthProvider provider;
    private String providerUserId;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
