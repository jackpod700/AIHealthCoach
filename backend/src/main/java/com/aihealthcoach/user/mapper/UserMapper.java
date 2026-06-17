package com.aihealthcoach.user.mapper;

import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.entity.UserProfile;
import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findUserByEmail(String email);
    void insertUser(User user);
    void insertUserProfile(UserProfile userProfile);
    UserProfile findUserProfileByUserId(Long id);
    void updateUserProfile(@Param("userId") Long userId, @Param("request") UserProfileUpdateRequest request);
    void updateUserProfileGoalType(@Param("userId") Long userId, @Param("goalType") String goalType);
    void updateUserProfileCurrentWeight(
            @Param("userId") Long userId,
            @Param("currentWeightKg") BigDecimal currentWeightKg
    );
}
