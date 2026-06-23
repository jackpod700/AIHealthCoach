package com.aihealthcoach.meal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class FoodContentHashGeneratorTest {

    @Test
    void generateIgnoresSourceKeyAndServingDescription() {
        String hash = FoodContentHashGenerator.generate(
                " Apple ",
                " Brand ",
                new BigDecimal("52.00"),
                new BigDecimal("0.170"),
                new BigDecimal("13.810"),
                new BigDecimal("0.260")
        );

        String sameFoodWithDifferentScale = FoodContentHashGenerator.generate(
                "apple",
                "brand",
                new BigDecimal("52"),
                new BigDecimal("0.17"),
                new BigDecimal("13.81"),
                new BigDecimal("0.26")
        );

        assertThat(hash).isEqualTo(sameFoodWithDifferentScale);
    }

    @Test
    void generateChangesWhenNutritionChanges() {
        String original = FoodContentHashGenerator.generate(
                "Apple",
                "Brand",
                new BigDecimal("52"),
                new BigDecimal("0.17"),
                new BigDecimal("13.81"),
                new BigDecimal("0.26")
        );

        String changed = FoodContentHashGenerator.generate(
                "Apple",
                "Brand",
                new BigDecimal("53"),
                new BigDecimal("0.17"),
                new BigDecimal("13.81"),
                new BigDecimal("0.26")
        );

        assertThat(changed).isNotEqualTo(original);
    }
}
