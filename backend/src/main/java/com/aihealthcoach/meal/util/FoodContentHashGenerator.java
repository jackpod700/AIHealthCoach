package com.aihealthcoach.meal.util;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public final class FoodContentHashGenerator {

    private FoodContentHashGenerator() {
    }

    public static String generate(
            String name,
            String brand,
            BigDecimal calories,
            BigDecimal fat,
            BigDecimal carbohydrate,
            BigDecimal protein
    ) {
        String raw = String.join("|",
                canonical(name),
                canonical(brand),
                decimal(calories),
                decimal(fat),
                decimal(carbohydrate),
                decimal(protein)
        );
        return digest(raw);
    }

    private static String canonical(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private static String decimal(BigDecimal value) {
        if (value == null) {
            return "";
        }

        return value.stripTrailingZeros().toPlainString();
    }

    private static String digest(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available.", exception);
        }
    }
}
