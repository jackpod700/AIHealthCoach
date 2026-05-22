package com.aihealthcoach.user.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aihealthcoach.user.dto.LoginRequest;
import com.aihealthcoach.user.dto.LoginResponse;
import com.aihealthcoach.user.dto.SignupRequest;
import com.aihealthcoach.user.dto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserProfileUpdateRequest;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.mapper.UserMapper;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userDao;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse signup(SignupRequest request) {
        User existingUser = userDao.findUserByEmail(request.getEmail());

        if (existingUser != null){
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
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
            throw new IllegalArgumentException("가입되지 않은 이메일입니다.");
        }

        if (!passwordEncoder.matches(request.getPassword(), existingUser.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 올바르지 않습니다.");
        }
        
        return LoginResponse.builder()
                .userId(existingUser.getId())
                .email(existingUser.getEmail())
                .nickname(existingUser.getNickname())
                .build();  
    }

    @Override
    public UserProfileResponse findProfile(Long userId) {
        UserProfile userProfile = userDao.findUserProfileByUserId(userId);

        if (userProfile == null){
            throw new IllegalArgumentException("유저 프로필이 존재하지 않는 userId 입니다.");
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
            throw new IllegalArgumentException("유저 프로필이 존재하지 않는 userId 입니다.");
        }

        userProfile.setHeightCm(request.getHeightCm());
        userProfile.setCurrentWeightKg(request.getCurrentWeightKg());
        userProfile.setTargetWeightKg(request.getTargetWeightKg());
        userProfile.setGoalType(request.getGoalType());

        userDao.updateUserProfile(userProfile);

        UserProfile updatedProfile = userDao.findUserProfileByUserId(userId);

        return UserProfileResponse.builder()
                .userId(updatedProfile.getUserId())
                .heightCm(updatedProfile.getHeightCm())
                .currentWeightKg(updatedProfile.getCurrentWeightKg())
                .targetWeightKg(updatedProfile.getTargetWeightKg())
                .goalType(updatedProfile.getGoalType())
                .updatedAt(userProfile.getUpdatedAt())
                .build();
    }
    
}
