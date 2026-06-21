package com.aihealthcoach.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

public class UserDto {

    @Builder
    public record LoginRequest(
            String email,
            String password
    ) {
    }

    @Builder
    public record LoginResponse (
        Long userId,
        String email,
        String nickname,
        String role,
        String accessToken) {
    }

    @Builder
    public record LoginResult(
            LoginResponse response,
            String refreshToken
    ) {
    }

    @Builder
    public record TokenRefreshResponse(
            String accessToken
    ) {
    }

    @Builder
    public record LogoutResponse(
            String message
    ) {
        private static final String DEFAULT_MESSAGE = "정상적으로 로그아웃되었습니다.";

        public LogoutResponse {
            if (message == null) {
                message = DEFAULT_MESSAGE;
            }
        }
    }

    @Builder
    public record SignupRequest(
            @NotBlank(message = "이메일은 필수입니다.")
            @Email(message = "이메일 형식이 올바르지 않습니다.")
            String email,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password,

            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname
    ) {
    }

    @Builder
    public record CurrentUserResponse(
            Long userId,
            String email,
            String nickname
    ) {
    }

    @Builder
    public record UserProfileResponse(
            Long userId,
            BigDecimal heightCm,
            BigDecimal currentWeightKg,
            BigDecimal targetWeightKg,
            String goalType,
            LocalDateTime updatedAt
    ) {
    }

    @Builder
    public record UserProfileUpdateRequest(
            @DecimalMin(value = "50.00", message = "키는 50cm 이상이어야 합니다.")
            @DecimalMax(value = "300.00", message = "키는 300cm 이하여야 합니다.")
            @Digits(integer = 3, fraction = 2, message = "키는 최대 999.99 형식이어야 합니다.")
            BigDecimal heightCm,

            @DecimalMin(value = "1.00", message = "현재 몸무게는 1kg 이상이어야 합니다.")
            @DecimalMax(value = "999.99", message = "현재 몸무게는 999.99kg 이하여야 합니다.")
            @Digits(integer = 3, fraction = 2, message = "현재 몸무게는 최대 999.99 형식이어야 합니다.")
            BigDecimal currentWeightKg,

            @DecimalMin(value = "1.00", message = "목표 몸무게는 1kg 이상이어야 합니다.")
            @DecimalMax(value = "999.99", message = "목표 몸무게는 999.99kg 이하여야 합니다.")
            @Digits(integer = 3, fraction = 2, message = "목표 몸무게는 최대 999.99 형식이어야 합니다.")
            BigDecimal targetWeightKg,

            String goalType
    ) {
    }

    public record UserNicknameUpdateRequest(
            @NotBlank(message = "닉네임은 필수입니다.")
            String nickname
    ) {
    }
}