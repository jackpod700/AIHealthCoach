package com.aihealthcoach.meal.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.aihealthcoach.meal.entity.FoodImportCandidate;
import com.aihealthcoach.meal.entity.FoodSearchMiss;

public class FoodImportCandidateDto {

    private FoodImportCandidateDto() {
    }

    public record FoodImportCandidatePageResponse(
            List<FoodImportCandidateGroupResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages
    ) {
    }

    public record FoodImportCandidateGroupResponse(
            Long searchMissId,
            String query,
            String normalizedQuery,
            String status,
            Integer missCount,
            OffsetDateTime lastRequestedAt,
            List<FoodImportCandidateResponse> candidates
    ) {
        public static FoodImportCandidateGroupResponse of(
                FoodSearchMiss searchMiss,
                List<FoodImportCandidate> candidates
        ) {
            return new FoodImportCandidateGroupResponse(
                    searchMiss.getId(),
                    searchMiss.getQuery(),
                    searchMiss.getNormalizedQuery(),
                    searchMiss.getStatus(),
                    searchMiss.getMissCount(),
                    searchMiss.getLastRequestedAt(),
                    candidates.stream()
                            .map(FoodImportCandidateResponse::fromEntity)
                            .toList()
            );
        }
    }

    public record FoodImportCandidateResponse(
            Long candidateId,
            Long searchMissId,
            String status,
            String sourceProvider,
            String sourceKey,
            String sourceUrl,
            String sourceFoodType,
            String name,
            String brand,
            String foodDescription,
            String servingDescription,
            BigDecimal servingSize,
            String servingUnit,
            BigDecimal calories,
            BigDecimal fat,
            BigDecimal carbohydrate,
            BigDecimal protein,
            String contentHash,
            Long duplicateFoodId,
            Long approvedFoodId,
            String rejectionReason,
            OffsetDateTime reviewedAt
    ) {
        public static FoodImportCandidateResponse fromEntity(FoodImportCandidate candidate) {
            return new FoodImportCandidateResponse(
                    candidate.getId(),
                    candidate.getSearchMissId(),
                    candidate.getStatus(),
                    candidate.getSourceProvider(),
                    candidate.getSourceKey(),
                    candidate.getSourceUrl(),
                    candidate.getSourceFoodType(),
                    candidate.getName(),
                    candidate.getBrand(),
                    candidate.getFoodDescription(),
                    candidate.getServingDescription(),
                    candidate.getServingSize(),
                    candidate.getServingUnit(),
                    candidate.getCalories(),
                    candidate.getFat(),
                    candidate.getCarbohydrate(),
                    candidate.getProtein(),
                    candidate.getContentHash(),
                    candidate.getDuplicateFoodId(),
                    candidate.getApprovedFoodId(),
                    candidate.getRejectionReason(),
                    candidate.getReviewedAt()
            );
        }
    }

    public record FoodImportCandidateRejectRequest(
            String rejectionReason
    ) {
    }
}
