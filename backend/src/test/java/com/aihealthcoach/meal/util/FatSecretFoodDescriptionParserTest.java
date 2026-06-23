package com.aihealthcoach.meal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.aihealthcoach.meal.util.FatSecretFoodDescriptionParser.ParsedFoodDescription;

class FatSecretFoodDescriptionParserTest {

    @Test
    void parseExtractsServingAndNutritionValues() {
        ParsedFoodDescription parsed = FatSecretFoodDescriptionParser.parse(
                "Per 100g - Calories: 52kcal | Fat: 0.17g | Carbs: 13.81g | Protein: 0.26g"
        );

        assertThat(parsed.servingDescription()).isEqualTo("100g");
        assertThat(parsed.servingSize()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(parsed.servingUnit()).isEqualTo("g");
        assertThat(parsed.calories()).isEqualByComparingTo(new BigDecimal("52"));
        assertThat(parsed.fat()).isEqualByComparingTo(new BigDecimal("0.17"));
        assertThat(parsed.carbohydrate()).isEqualByComparingTo(new BigDecimal("13.81"));
        assertThat(parsed.protein()).isEqualByComparingTo(new BigDecimal("0.26"));
        assertThat(parsed.hasRequiredNutrition()).isTrue();
    }

    @Test
    void parseMarksDescriptionWithoutNutritionAsInvalidCandidate() {
        ParsedFoodDescription parsed = FatSecretFoodDescriptionParser.parse("Per 1 serving");

        assertThat(parsed.servingDescription()).isEqualTo("1 serving");
        assertThat(parsed.hasRequiredNutrition()).isFalse();
    }
}
