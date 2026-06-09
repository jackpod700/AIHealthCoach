package com.aihealthcoach.meal.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Food {
    private Long foodId;
    private String foodName;
    private String brand;
    private BigDecimal servingSize;
    private String servingUnit;
    private BigDecimal calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;
}
