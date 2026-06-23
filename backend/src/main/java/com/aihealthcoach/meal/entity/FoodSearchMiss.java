package com.aihealthcoach.meal.entity;

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
public class FoodSearchMiss {
    private Long id;
    private String query;
    private String normalizedQuery;
    private String status;
    private Integer missCount;
    private Long firstUserId;
    private Long lastUserId;
    private OffsetDateTime lastRequestedAt;
    private OffsetDateTime processingStartedAt;
    private OffsetDateTime processedAt;
    private Integer retryCount;
    private String lastFailureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
