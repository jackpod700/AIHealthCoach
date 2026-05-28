package com.aihealthcoach.user.mapper;

import com.aihealthcoach.user.dto.UserProfileUpdateRequest;
import com.aihealthcoach.user.entity.User;
import com.aihealthcoach.user.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findUserByEmail(String email);
    void insertUser(User user);
    void insertUserProfile(UserProfile userProfile);
    UserProfile findUserProfileByUserId(Long id);
    void updateUserProfile(@Param("userId") Long userId, @Param("request") UserProfileUpdateRequest request);
}
