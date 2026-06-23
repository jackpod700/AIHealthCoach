package com.aihealthcoach.meal.batch;

public record FoodImportBatchResult(
        Long runId,
        String status,
        int pickedCount,
        int candidateCount,
        int failureCount,
        String failureReason
) {
}
