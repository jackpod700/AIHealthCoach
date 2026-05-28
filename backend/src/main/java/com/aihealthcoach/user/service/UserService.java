package com.aihealthcoach.user.service;

import com.aihealthcoach.user.dto.LoginRequest;
import com.aihealthcoach.user.dto.LoginResponse;
import com.aihealthcoach.user.dto.SignupRequest;
import com.aihealthcoach.user.dto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserProfileUpdateRequest;

public interface UserService {
    LoginResponse signup(SignupRequest request);

    LoginResponse login(LoginRequest request);

    UserProfileResponse findProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request);
}
