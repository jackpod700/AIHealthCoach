package com.aihealthcoach.user.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    private BigDecimal heightCm;
    private BigDecimal currentWeightKg;
    private BigDecimal targetWeightKg;
    private String goalType;
}
