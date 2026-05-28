package com.aihealthcoach.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.user.dto.LoginRequest;
import com.aihealthcoach.user.dto.LoginResponse;
import com.aihealthcoach.user.dto.SignupRequest;
import com.aihealthcoach.user.dto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserProfileUpdateRequest;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.exception.UserException;
import com.aihealthcoach.user.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LoginResponse signup(SignupRequest request) {
        User existingUser = userDao.findUserByEmail(request.getEmail());

        if (existingUser != null){
            throw UserException.duplicateEmail();
        }

        User newUser = User.builder()
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .nickname(request.getNickname())
                        .build();
        
        userDao.insertUser(newUser);

        User savedUser = userDao.findUserByEmail(request.getEmail());

        UserProfile profile = UserProfile.builder()
                                .userId(savedUser.getId())
                                .build();

        userDao.insertUserProfile(profile);

        return LoginResponse.builder()
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .nickname(savedUser.getNickname())
                .build();            
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User existingUser = userDao.findUserByEmail(request.getEmail());

        if (existingUser == null){
            throw UserException.userNotFound();
        }

        if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw UserException.invalidPassword();
        }

        String accessToken = jwtTokenProvider.createAccessToken(existingUser.getId());
        
        return LoginResponse.builder()
                .userId(existingUser.getId())
                .email(existingUser.getEmail())
                .nickname(existingUser.getNickname())
                .accessToken(accessToken)
                .build();  
    }

    @Override
    public UserProfileResponse findProfile(Long userId) {
        UserProfile userProfile = userDao.findUserProfileByUserId(userId);

        if (userProfile == null){
            throw UserException.profileNotFound();
        }

        return UserProfileResponse.builder()
                .userId(userProfile.getUserId())
                .heightCm(userProfile.getHeightCm())
                .currentWeightKg(userProfile.getCurrentWeightKg())
                .targetWeightKg(userProfile.getTargetWeightKg())
                .goalType(userProfile.getGoalType())
                .updatedAt(userProfile.getUpdatedAt())
                .build();
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UserProfileUpdateRequest request) {
        UserProfile userProfile = userDao.findUserProfileByUserId(userId);

        if (userProfile == null){
            throw UserException.profileNotFound();
        }

        userDao.updateUserProfile(userId, request);

        UserProfile updatedProfile = userDao.findUserProfileByUserId(userId);

        return UserProfileResponse.builder()
                .userId(updatedProfile.getUserId())
                .heightCm(updatedProfile.getHeightCm())
                .currentWeightKg(updatedProfile.getCurrentWeightKg())
                .targetWeightKg(updatedProfile.getTargetWeightKg())
                .goalType(updatedProfile.getGoalType())
                .updatedAt(updatedProfile.getUpdatedAt())
                .build();
    }
    
}
