package com.aihealthcoach.common.fatsecret;

import java.util.List;

public class FatSecretDto {

    private FatSecretDto() {
    }

    public record FatSecretSearchTestResponse(
            boolean connected,
            String query,
            Integer totalResults,
            Integer pageNumber,
            Integer maxResults,
            List<FatSecretFoodItemResponse> items
    ) {
    }

    public record FatSecretFoodItemResponse(
            String foodId,
            String foodName,
            String foodType,
            String brandName,
            String foodUrl,
            String foodDescription
    ) {
    }

    record FatSecretTokenResponse(
            String access_token,
            String token_type,
            Long expires_in
    ) {
    }
}
