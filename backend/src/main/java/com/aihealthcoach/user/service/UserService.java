package com.aihealthcoach.user.service;

import com.aihealthcoach.user.dto.UserDto.CurrentUserResponse;
import com.aihealthcoach.user.dto.UserDto.LoginRequest;
import com.aihealthcoach.user.dto.UserDto.LoginResponse;
import com.aihealthcoach.user.dto.UserDto.LoginResult;
import com.aihealthcoach.user.dto.UserDto.SignupRequest;
import com.aihealthcoach.user.dto.UserDto.TokenRefreshResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;

public interface UserService {

    LoginResponse signup(SignupRequest request);

    LoginResult login(LoginRequest request);

    void logout(String accessToken, String refreshToken);

    TokenRefreshResponse refreshAccessToken(String refreshToken);

    CurrentUserResponse findCurrentUser(Long userId);

    UserProfileResponse findProfile(Long userId);

    UserProfileResponse findProfileIfExists(Long userId);

    UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request);

    void updateNickname(Long userId, String nickname);
}