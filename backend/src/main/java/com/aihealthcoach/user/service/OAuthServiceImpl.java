package com.aihealthcoach.user.service;

import com.aihealthcoach.user.dto.OAuthUserDto;
import com.aihealthcoach.user.entity.OAuthAccount;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final UserMapper userMapper;

    @Override
    @Transactional
    public User findOrCreateOAuthUser(OAuthUserDto userDto) {
        OAuthAccount oauthAccount = userMapper.findOAuthAccount(
                userDto.provider().name(),
                userDto.providerUserId()
        );

        if (oauthAccount != null) {
            return userMapper.findUserById(oauthAccount.getUserId());
        }

        User user = findOrCreateUser(userDto);

        OAuthAccount newOAuthAccount = OAuthAccount.builder()
                .userId(user.getId())
                .provider(userDto.provider())
                .providerUserId(userDto.providerUserId())
                .email(userDto.email())
                .build();

        userMapper.insertOAuthAccount(newOAuthAccount);

        return user;
    }

    private User findOrCreateUser(OAuthUserDto userDto) {
        if (userDto.email() != null) {
            User existingUser = userMapper.findUserByEmail(userDto.email());

            if (existingUser != null) {
                return existingUser;
            }
        }

        User user = User.builder()
                .email(userDto.email())
                .password(null)
                .nickname(resolveNickname(userDto))
                .build();

        Long userId = userMapper.insertOAuthUser(user);
        user.setId(userId);

        UserProfile userProfile = UserProfile.builder()
                .userId(userId)
                .build();

        userMapper.insertUserProfile(userProfile);

        return user;
    }

    private String resolveNickname(OAuthUserDto userDto) {
        if (userDto.nickname() != null && !userDto.nickname().isBlank()) {
            return userDto.nickname();
        }

        if (userDto.email() != null && userDto.email().contains("@")) {
            return userDto.email().substring(0, userDto.email().indexOf("@"));
        }

        return userDto.provider().name().toLowerCase() + "_" + userDto.providerUserId();
    }
}