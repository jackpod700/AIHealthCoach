package com.aihealthcoach.user.service;

import com.aihealthcoach.user.dto.OAuthUserDto;
import com.aihealthcoach.user.entity.User;

public interface OAuthService {
    User findOrCreateOAuthUser(OAuthUserDto userDto);
}
