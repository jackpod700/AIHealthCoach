package com.aihealthcoach.meal.util;

import java.util.Arrays;
import java.util.List;

public final class FoodSearchQuery {

    private FoodSearchQuery() {
    }

    public record Token(
            String value,
            String compactValue
    ) {
    }

    public static String normalize(String query) {
        if (query == null) {
            return "";
        }

        return query.trim().replaceAll("\\s+", " ");
    }

    public static List<Token> tokens(String query) {
        String normalizedQuery = normalize(query);

        if (normalizedQuery.isBlank()) {
            return List.of();
        }

        return Arrays.stream(normalizedQuery.split(" "))
                .filter(token -> !token.isBlank())
                .map(token -> new Token(token, compact(token)))
                .toList();
    }

    public static String compact(String value) {
        if (value == null) {
            return "";
        }

        return value.replaceAll("\\s+", "");
    }
}
