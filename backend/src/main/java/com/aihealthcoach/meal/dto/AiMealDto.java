package com.aihealthcoach.meal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.aihealthcoach.chat.dto.ChatDto.ChatMessageResponse;
import com.aihealthcoach.meal.dto.MealDto.DailyMealResponse;
import com.aihealthcoach.meal.entity.Food;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class AiMealDto {

    public record ExtractedMealResult(
            boolean mealIntent,
            LocalDate mealDate,
            String mealType,
            List<ExtractedMealItem> items
    ) {
        public static ExtractedMealResult noMeal() {
            return new ExtractedMealResult(false, null, null, List.of());
        }
    }

    public record ExtractedMealItem(
            String name,
            BigDecimal quantity
    ) {
    }

    public record MealProposalResponse(
            LocalDate mealDate,
            String mealType,
            List<MealProposalItemResponse> items,
            List<String> defaultsApplied
    ) {
    }

    public record MealProposalItemResponse(
            String extractedName,
            BigDecimal quantity,
            List<FoodCandidateResponse> candidates
    ) {
    }

    public record FoodCandidateResponse(
            Long foodId,
            String foodName,
            String brand,
            BigDecimal servingSize,
            String servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrate,
            BigDecimal protein,
            BigDecimal fat
    ) {
        public static FoodCandidateResponse fromRow(Food row) {
            return new FoodCandidateResponse(
                    row.getFoodId(),
                    row.getFoodName(),
                    row.getBrand(),
                    row.getServingSize(),
                    row.getServingUnit(),
                    row.getCalories(),
                    row.getCarbohydrate(),
                    row.getProtein(),
                    row.getFat()
            );
        }
    }

    public record ConfirmMealProposalRequest(
            @NotNull LocalDate mealDate,
            @NotBlank String mealType,
            @NotEmpty List<@Valid ConfirmMealProposalItemRequest> items
    ) {
    }

    public record ConfirmMealProposalItemRequest(
            @NotNull Long foodId,
            @NotNull @Positive BigDecimal quantity
    ) {
    }

    public record ConfirmMealProposalResponse(
            List<ChatMessageResponse> messages,
            DailyMealResponse dailyMeal
    ) {
    }
}
