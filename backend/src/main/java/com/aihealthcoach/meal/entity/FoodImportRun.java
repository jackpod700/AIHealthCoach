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
public class FoodImportRun {
    private Long id;
    private String status;
    private Integer pickedCount;
    private Integer candidateCount;
    private Integer failureCount;
    private String failureReason;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
