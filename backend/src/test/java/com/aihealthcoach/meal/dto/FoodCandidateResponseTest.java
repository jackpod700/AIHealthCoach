package com.aihealthcoach.meal.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.aihealthcoach.meal.dto.AiMealDto.FoodCandidateResponse;
import com.aihealthcoach.meal.entity.Food;

class FoodCandidateResponseTest {

    @Test
    void fromRowMapsServingBasis() {
        Food row = new Food();
        row.setFoodCode("1234567890123456789");
        row.setFoodName("rice");
        row.setManufacturer("maker");
        row.setServingSize(new BigDecimal("100.00"));
        row.setServingUnit("g");
        row.setCalories(new BigDecimal("150.00"));
        row.setCarbohydrate(new BigDecimal("30.00"));
        row.setProtein(new BigDecimal("3.00"));
        row.setFat(new BigDecimal("1.00"));

        FoodCandidateResponse response = FoodCandidateResponse.fromRow(row);

        assertThat(response.servingSize()).isEqualByComparingTo("100.00");
        assertThat(response.servingUnit()).isEqualTo("g");
    }
}
