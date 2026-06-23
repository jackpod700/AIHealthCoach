package com.aihealthcoach.meal.dto;

public class FoodSearchMissDto {

    private FoodSearchMissDto() {
    }

    public record FoodSearchMissCreateRequest(
            String query
    ) {
    }

    public record FoodSearchMissResponse(
            boolean accepted,
            String status,
            Long searchMissId,
            String query,
            String normalizedQuery,
            Integer missCount
    ) {
    }
}
