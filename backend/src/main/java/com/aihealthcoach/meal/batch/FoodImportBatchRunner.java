package com.aihealthcoach.meal.batch;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.aihealthcoach.meal.entity.FoodSearchMiss;
import com.aihealthcoach.meal.mapper.FoodImportMapper;
import com.aihealthcoach.meal.service.FoodImportCandidateService;
import com.aihealthcoach.meal.service.FoodSearchMissService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodImportBatchRunner {

    private static final int MAX_FAILURE_REASON_LENGTH = 1000;

    private final FoodBatchProperties properties;
    private final FoodSearchMissService foodSearchMissService;
    private final FoodImportCandidateService foodImportCandidateService;
    private final FoodImportMapper foodImportMapper;

    public FoodImportBatchResult runOnce() {
        Long runId = foodImportMapper.insertFoodImportRun();
        int pickedCount = 0;
        int candidateCount = 0;
        int failureCount = 0;
        String failureReason = null;

        try {
            List<FoodSearchMiss> misses = foodSearchMissService.pickPendingMisses(
                    properties.effectiveChunkSize(),
                    properties.effectiveMaxRetries()
            );
            pickedCount = misses.size();

            for (FoodSearchMiss miss : misses) {
                try {
                    candidateCount += foodImportCandidateService.importCandidates(miss, runId);
                } catch (RuntimeException exception) {
                    failureCount++;
                    String message = truncate(exception.getMessage());
                    failureReason = message;
                    foodSearchMissService.markFailed(miss.getId(), message);
                    log.warn("Food import candidate collection failed. searchMissId={}", miss.getId(), exception);
                }
            }

            String status = status(pickedCount, failureCount);
            foodImportMapper.completeFoodImportRun(
                    runId,
                    status,
                    pickedCount,
                    candidateCount,
                    failureCount,
                    failureReason
            );
            return new FoodImportBatchResult(runId, status, pickedCount, candidateCount, failureCount, failureReason);
        } catch (RuntimeException exception) {
            failureReason = truncate(exception.getMessage());
            foodImportMapper.completeFoodImportRun(
                    runId,
                    "FAILED",
                    pickedCount,
                    candidateCount,
                    Math.max(1, failureCount),
                    failureReason
            );
            throw exception;
        }
    }

    private String status(int pickedCount, int failureCount) {
        if (failureCount == 0) {
            return "SUCCESS";
        }
        if (failureCount >= pickedCount) {
            return "FAILED";
        }
        return "PARTIAL_FAILED";
    }

    private String truncate(String value) {
        String normalized = Objects.toString(value, "Unknown failure.").trim();
        if (normalized.length() <= MAX_FAILURE_REASON_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
