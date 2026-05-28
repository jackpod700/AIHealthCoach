package com.aihealthcoach.user.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {
    private Long id;
    private Long userId;
    private BigDecimal heightCm;
    private BigDecimal currentWeightKg;
    private BigDecimal targetWeightKg;
    private String goalType;
    private LocalDateTime updatedAt;
}
