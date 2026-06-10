package com.aihealthcoach.meal.dto;

import java.math.BigDecimal;
import java.util.List;

import com.aihealthcoach.meal.entity.Food;

public class FoodDto {

    public record FoodSearchPageResponse(
            List<FoodGroupResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages
    ) {
    }

    public record FoodGroupResponse(
            String sourceKey,
            String foodName,
            String brand,
            List<FoodServingResponse> servings
    ) {
    }

    public record FoodServingResponse(
            Long foodId,
            String servingDescription,
            BigDecimal servingSize,
            String servingUnit,
            BigDecimal calories,
            BigDecimal carbohydrate,
            BigDecimal protein,
            BigDecimal fat
    ) {
        public static FoodServingResponse fromRow(Food row) {
            return new FoodServingResponse(
                    row.getFoodId(),
                    row.getServingDescription(),
                    row.getServingSize(),
                    row.getServingUnit(),
                    row.getCalories(),
                    row.getCarbohydrate(),
                    row.getProtein(),
                    row.getFat()
            );
        }
    }
}
