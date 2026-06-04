package com.aihealthcoach.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    public record SignupRequest (
        String email,
        String password,
        String nickname) {
    }

    @Builder
    public record UserProfileResponse (
        Long userId,
        BigDecimal heightCm,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg,
        String goalType,
        LocalDateTime updatedAt) {
    }

    @Builder
    public record UserProfileUpdateRequest (
        BigDecimal heightCm,
        BigDecimal currentWeightKg,
        BigDecimal targetWeightKg,
        String goalType
    ) {
    }

}
