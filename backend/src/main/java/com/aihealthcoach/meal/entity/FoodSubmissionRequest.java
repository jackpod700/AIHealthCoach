package com.aihealthcoach.meal.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FoodSubmissionRequest {
    private Long id;
    private Long submittedByUserId;
    private String submitterNickname;
    private String submitterEmail;
    private String status;
    private String name;
    private String brand;
    private String servingDescription;
    private BigDecimal servingSize;
    private String servingUnit;
    private BigDecimal calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;
    private String adminNote;
    private String rejectionReason;
    private Long approvedFoodId;
    private Long reviewedByAdminId;
    private OffsetDateTime submittedAt;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
