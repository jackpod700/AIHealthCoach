package com.aihealthcoach.user.mapper;

import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;
import com.aihealthcoach.user.entity.OAuthAccount;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.entity.UserProfile;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findUserByEmail(String email);

    User findUserById(Long id);

    void insertUser(User user);

    Long insertOAuthUser(User user);

    void insertUserProfile(UserProfile userProfile);

    UserProfile findUserProfileByUserId(@Param("userId") Long userId);

    void updateUserProfileGoalType(
            @Param("userId") Long userId,
            @Param("goalType") String goalType
    );

    void updateUserProfileCurrentWeight(
            @Param("userId") Long userId,
            @Param("currentWeightKg") BigDecimal currentWeightKg
    );

    void updateUserProfile(
            @Param("userId") Long userId,
            @Param("request") UserProfileUpdateRequest request
    );

    OAuthAccount findOAuthAccount(
            @Param("provider") String provider,
            @Param("providerUserId") String providerUserId
    );

    void insertOAuthAccount(OAuthAccount oauthAccount);

    void updateUserNickname(
            @Param("userId") Long userId,
            @Param("nickname") String nickname
    );
}
