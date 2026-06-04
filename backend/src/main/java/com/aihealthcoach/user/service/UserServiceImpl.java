package com.aihealthcoach.user.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.user.dto.UserDto.LoginRequest;
import com.aihealthcoach.user.dto.UserDto.LoginResponse;
import com.aihealthcoach.user.dto.UserDto.LoginResult;
import com.aihealthcoach.user.dto.UserDto.SignupRequest;
import com.aihealthcoach.user.dto.UserDto.TokenRefreshResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;
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
    private final TokenRedisRepository tokenRedisRepository;

    @Override
    public LoginResponse signup(SignupRequest request) {
        User existingUser = userDao.findUserByEmail(request.email());

        if (existingUser != null){
            throw UserException.duplicateEmail();
        }

        User newUser = User.builder()
                        .email(request.email())
                        .password(passwordEncoder.encode(request.password()))
                        .nickname(request.nickname())
                        .build();
        
        userDao.insertUser(newUser);

        User savedUser = userDao.findUserByEmail(request.email());

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
    public LoginResult login(LoginRequest request) {
        User existingUser = userDao.findUserByEmail(request.email());

        if (existingUser == null){
            throw UserException.userNotFound();
        }

        if (!passwordEncoder.matches(request.password(), existingUser.getPassword())) {
            throw UserException.invalidPassword();
        }

        String accessToken = jwtTokenProvider.createAccessToken(existingUser.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(existingUser.getId());
        String refreshTokenId = jwtTokenProvider.getTokenId(refreshToken);

        tokenRedisRepository.saveRefreshToken(
            existingUser.getId(), 
            refreshTokenId, 
            refreshToken, 
            jwtTokenProvider.getRemaining(refreshToken));
        
        LoginResponse response = LoginResponse.builder()
            .userId(existingUser.getId())
            .email(existingUser.getEmail())
            .nickname(existingUser.getNickname())
            .accessToken(accessToken)
            .build();

        return LoginResult.builder()
            .response(response)
            .refreshToken(refreshToken)
            .build(); 
    }

    @Override
    public TokenRefreshResponse refreshAccessToken(String refreshToken) {
        jwtTokenProvider.validateRefreshToken(refreshToken);

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String refreshTokenId = jwtTokenProvider.getTokenId(refreshToken);

        String savedRefreshToken = tokenRedisRepository
            .findRefreshToken(userId, refreshTokenId)
            .orElseThrow(UserException::invalidToken);

        if (!savedRefreshToken.equals(refreshToken)) {
            throw UserException.invalidToken();
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);

        return TokenRefreshResponse.builder()
            .accessToken(newAccessToken)
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
