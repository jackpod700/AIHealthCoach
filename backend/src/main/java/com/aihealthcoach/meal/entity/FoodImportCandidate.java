package com.aihealthcoach.meal.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FoodImportCandidate {
    private Long id;
    private Long searchMissId;
    private Long importRunId;
    private String searchQuery;
    private String normalizedQuery;
    private String status;
    private String sourceProvider;
    private String sourceKey;
    private String sourceUrl;
    private String sourceFoodType;
    private String name;
    private String brand;
    private String foodDescription;
    private String servingDescription;
    private BigDecimal servingSize;
    private String servingUnit;
    private BigDecimal calories;
    private BigDecimal fat;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private String contentHash;
    private Long duplicateFoodId;
    private Long approvedFoodId;
    private String rejectionReason;
    private Long reviewedByAdminId;
    private OffsetDateTime reviewedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
