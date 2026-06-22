package com.aihealthcoach.user.dto;

import com.aihealthcoach.user.entity.OAuthProvider;
import lombok.Builder;

@Builder
public record OAuthUserDto (
        OAuthProvider provider,
        String providerUserId,
        String email,
        String nickname
){}
