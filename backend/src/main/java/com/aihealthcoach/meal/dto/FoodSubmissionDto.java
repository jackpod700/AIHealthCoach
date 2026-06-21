package com.aihealthcoach.meal.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.aihealthcoach.meal.entity.FoodSubmissionRequest;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

public class FoodSubmissionDto {

    public record FoodSubmissionCreateRequest(
            @NotBlank(message = "Food name is required.")
            @Size(max = 255, message = "Food name must be 255 characters or less.")
            String name,

            @Size(max = 255, message = "Brand must be 255 characters or less.")
            String brand,

            @Size(max = 100, message = "Serving description must be 100 characters or less.")
            String servingDescription,

            @DecimalMin(value = "0", inclusive = false, message = "Serving size must be greater than 0.")
            BigDecimal servingSize,

            @Size(max = 20, message = "Serving unit must be 20 characters or less.")
            String servingUnit,

            @NotNull(message = "Calories are required.")
            @DecimalMin(value = "0", message = "Calories must be 0 or greater.")
            BigDecimal calories,

            @NotNull(message = "Carbohydrate is required.")
            @DecimalMin(value = "0", message = "Carbohydrate must be 0 or greater.")
            BigDecimal carbohydrate,

            @NotNull(message = "Protein is required.")
            @DecimalMin(value = "0", message = "Protein must be 0 or greater.")
            BigDecimal protein,

            @NotNull(message = "Fat is required.")
            @DecimalMin(value = "0", message = "Fat must be 0 or greater.")
            BigDecimal fat
    ) {
    }

    public record FoodSubmissionReviewRequest(
            @NotBlank(message = "Food name is required.")
            @Size(max = 255, message = "Food name must be 255 characters or less.")
            String name,

            @Size(max = 255, message = "Brand must be 255 characters or less.")
            String brand,

            @Size(max = 100, message = "Serving description must be 100 characters or less.")
            String servingDescription,

            @DecimalMin(value = "0", inclusive = false, message = "Serving size must be greater than 0.")
            BigDecimal servingSize,

            @Size(max = 20, message = "Serving unit must be 20 characters or less.")
            String servingUnit,

            @NotNull(message = "Calories are required.")
            @DecimalMin(value = "0", message = "Calories must be 0 or greater.")
            BigDecimal calories,

            @NotNull(message = "Carbohydrate is required.")
            @DecimalMin(value = "0", message = "Carbohydrate must be 0 or greater.")
            BigDecimal carbohydrate,

            @NotNull(message = "Protein is required.")
            @DecimalMin(value = "0", message = "Protein must be 0 or greater.")
            BigDecimal protein,

            @NotNull(message = "Fat is required.")
            @DecimalMin(value = "0", message = "Fat must be 0 or greater.")
            BigDecimal fat,

            @Size(max = 1000, message = "Admin note must be 1000 characters or less.")
            String adminNote
    ) {
    }

    public record FoodSubmissionRejectRequest(
            @NotBlank(message = "Rejection reason is required.")
            @Size(max = 1000, message = "Rejection reason must be 1000 characters or less.")
            String rejectionReason
    ) {
    }


    public record FoodSubmissionPageResponse(
            List<FoodSubmissionResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages
    ) {
    }

    public record FoodSubmissionResponse(
            Long id,
            Long submittedByUserId,
            String submitterNickname,
            String submitterEmail,
            String status,
            String name,
            String brand,
            String servingDescription,
            BigDecimal servingSize,
            String servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrate,
            BigDecimal protein,
            BigDecimal fat,
            String adminNote,
            String rejectionReason,
            Long approvedFoodId,
            Long reviewedByAdminId,
            OffsetDateTime submittedAt,
            OffsetDateTime reviewedAt
    ) {
        public static FoodSubmissionResponse fromEntity(FoodSubmissionRequest request) {
            return new FoodSubmissionResponse(
                    request.getId(),
                    request.getSubmittedByUserId(),
                    request.getSubmitterNickname(),
                    request.getSubmitterEmail(),
                    request.getStatus(),
                    request.getName(),
                    request.getBrand(),
                    request.getServingDescription(),
                    request.getServingSize(),
                    request.getServingUnit(),
                    request.getCalories(),
                    request.getCarbohydrate(),
                    request.getProtein(),
                    request.getFat(),
                    request.getAdminNote(),
                    request.getRejectionReason(),
                    request.getApprovedFoodId(),
                    request.getReviewedByAdminId(),
                    request.getSubmittedAt(),
                    request.getReviewedAt()
            );
        }
    }
}
