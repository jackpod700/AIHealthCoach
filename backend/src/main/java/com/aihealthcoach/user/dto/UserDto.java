package com.aihealthcoach.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Builder;

public class UserDto {

    @Builder
    public record LoginRequest (
        String email,
        String password
    ){
    }

    @Builder
    public record LoginResponse (
        Long userId,
        String email,
        String nickname,
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
    public record SignupRequest (
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        String email,
        @NotBlank(message = "비밀번호는 필수입니다.")
        String password,
        @NotBlank(message = "닉네임은 필수입니다.")
        String nickname) {
    }

    @Builder
    public record UserProfileResponse (
        Long userId,
        BigDecimal heightCm,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg,
        String goalType,
        String gender,
        Integer age,
        LocalDateTime updatedAt) {
    }

    @Builder
    public record UserProfileUpdateRequest (
        BigDecimal heightCm,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg,
        String goalType,
        @Pattern(regexp = "MALE|FEMALE", message = "gender는 MALE 또는 FEMALE이어야 합니다.")
        String gender,
        @Positive(message = "age는 1 이상이어야 합니다.")
        Integer age
    ) {
    }

}
