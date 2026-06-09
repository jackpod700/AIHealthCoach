package com.aihealthcoach.meal.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MealFood {
    private Long mealId;
    private String mealType;
    private LocalDate mealDate;
    private Long foodId;
    private String foodName;
    private String brand;
    private BigDecimal servingSize;
    private String servingUnit;
    private BigDecimal quantity;
    private BigDecimal calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;
}
