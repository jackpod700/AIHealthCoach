package com.aihealthcoach.meal.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.aihealthcoach.meal.util.FoodSearchQuery.Token;

class FoodSearchQueryTest {

    @Test
    void tokensIncludeCompactValueForWhitespaceInsensitiveSearch() {
        List<Token> tokens = FoodSearchQuery.tokens(" 프랭크   치즈버거 ");

        assertThat(tokens).containsExactly(
                new FoodSearchQuery.Token("프랭크", "프랭크"),
                new FoodSearchQuery.Token("치즈버거", "치즈버거")
        );
    }

    @Test
    void compactRemovesWhitespace() {
        assertThat(FoodSearchQuery.compact("치즈 버거")).isEqualTo("치즈버거");
    }
}
