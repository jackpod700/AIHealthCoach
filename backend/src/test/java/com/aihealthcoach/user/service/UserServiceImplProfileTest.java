package com.aihealthcoach.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aihealthcoach.common.auth.JwtTokenProvider;
import com.aihealthcoach.common.auth.TokenRedisRepository;
import com.aihealthcoach.user.dto.UserDto.UserProfileResponse;
import com.aihealthcoach.user.dto.UserDto.UserProfileUpdateRequest;
import com.aihealthcoach.user.entity.UserProfile;
import com.aihealthcoach.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplProfileTest {

    private static final Long USER_ID = 1L;

    @Mock
    private UserMapper userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRedisRepository tokenRedisRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findProfileIncludesGenderAndAge() {
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 15, 12, 0);
        when(userDao.findUserProfileByUserId(USER_ID)).thenReturn(UserProfile.builder()
                .userId(USER_ID)
                .heightCm(new BigDecimal("172.50"))
                .currentWeightKg(new BigDecimal("68.40"))
                .targetWeightKg(new BigDecimal("65.00"))
                .goalType("WEIGHT_LOSS")
                .gender("FEMALE")
                .age(27)
                .updatedAt(updatedAt)
                .build());

        UserProfileResponse response = userService.findProfile(USER_ID);

        assertThat(response.gender()).isEqualTo("FEMALE");
        assertThat(response.age()).isEqualTo(27);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }

    @Test
    void updateProfileReturnsUpdatedGenderAndAge() {
        UserProfileUpdateRequest request = UserProfileUpdateRequest.builder()
                .gender("MALE")
                .age(32)
                .build();
        when(userDao.findUserProfileByUserId(USER_ID))
                .thenReturn(UserProfile.builder().userId(USER_ID).build())
                .thenReturn(UserProfile.builder()
                        .userId(USER_ID)
                        .gender("MALE")
                        .age(32)
                        .build());

        UserProfileResponse response = userService.updateProfile(USER_ID, request);

        verify(userDao).updateUserProfile(USER_ID, request);
        assertThat(response.gender()).isEqualTo("MALE");
        assertThat(response.age()).isEqualTo(32);
    }
}
