package com.aihealthcoach.meal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MealFoodRow {
    private Long mealId;
    private String mealType;
    private LocalDate mealDate;
    private String foodCode;
    private String foodName;
    private String manufacturer;
    private BigDecimal servingSize;
    private String servingUnit;
    private BigDecimal quantity;
    private BigDecimal calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;
}
