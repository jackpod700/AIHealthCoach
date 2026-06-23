package com.aihealthcoach.meal.util;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FatSecretFoodDescriptionParser {

    private static final Pattern NUTRIENT_PATTERN = Pattern.compile(
            "(?i)(calories|fat|carbs|carbohydrate|protein)\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)"
    );
    private static final Pattern SERVING_PATTERN = Pattern.compile(
            "(?i)^per\\s+(.+?)(?:\\s+-\\s+.*)?$"
    );
    private static final Pattern SERVING_SIZE_UNIT_PATTERN = Pattern.compile(
            "^([0-9]+(?:\\.[0-9]+)?)\\s*([\\p{L}%]+.*)?$"
    );

    private FatSecretFoodDescriptionParser() {
    }

    public static ParsedFoodDescription parse(String foodDescription) {
        String servingDescription = parseServingDescription(foodDescription);
        ParsedServing parsedServing = parseServing(servingDescription);
        Nutrients nutrients = parseNutrients(foodDescription);

        return new ParsedFoodDescription(
                servingDescription,
                parsedServing.size(),
                parsedServing.unit(),
                nutrients.calories(),
                nutrients.fat(),
                nutrients.carbohydrate(),
                nutrients.protein()
        );
    }

    private static String parseServingDescription(String foodDescription) {
        if (foodDescription == null || foodDescription.isBlank()) {
            return null;
        }

        Matcher matcher = SERVING_PATTERN.matcher(foodDescription.trim());
        if (!matcher.matches()) {
            return null;
        }

        String serving = matcher.group(1).trim();
        return serving.isBlank() ? null : serving;
    }

    private static ParsedServing parseServing(String servingDescription) {
        if (servingDescription == null || servingDescription.isBlank()) {
            return new ParsedServing(null, null);
        }

        Matcher matcher = SERVING_SIZE_UNIT_PATTERN.matcher(servingDescription.trim());
        if (!matcher.matches()) {
            return new ParsedServing(null, null);
        }

        BigDecimal size = decimal(matcher.group(1));
        String unit = matcher.group(2) == null ? null : matcher.group(2).trim();
        return new ParsedServing(size, unit == null || unit.isBlank() ? null : unit);
    }

    private static Nutrients parseNutrients(String foodDescription) {
        if (foodDescription == null || foodDescription.isBlank()) {
            return new Nutrients(null, null, null, null);
        }

        BigDecimal calories = null;
        BigDecimal fat = null;
        BigDecimal carbohydrate = null;
        BigDecimal protein = null;

        Matcher matcher = NUTRIENT_PATTERN.matcher(foodDescription);
        while (matcher.find()) {
            String key = matcher.group(1).toLowerCase(Locale.ROOT);
            BigDecimal value = decimal(matcher.group(2));

            switch (key) {
                case "calories" -> calories = value;
                case "fat" -> fat = value;
                case "carbs", "carbohydrate" -> carbohydrate = value;
                case "protein" -> protein = value;
                default -> {
                    // ignored
                }
            }
        }

        return new Nutrients(calories, fat, carbohydrate, protein);
    }

    private static BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return new BigDecimal(value.trim());
    }

    public record ParsedFoodDescription(
            String servingDescription,
            BigDecimal servingSize,
            String servingUnit,
            BigDecimal calories,
            BigDecimal fat,
            BigDecimal carbohydrate,
            BigDecimal protein
    ) {
        public boolean hasRequiredNutrition() {
            return calories != null
                    && fat != null
                    && carbohydrate != null
                    && protein != null;
        }
    }

    private record ParsedServing(BigDecimal size, String unit) {
    }

    private record Nutrients(
            BigDecimal calories,
            BigDecimal fat,
            BigDecimal carbohydrate,
            BigDecimal protein
    ) {
    }
}
