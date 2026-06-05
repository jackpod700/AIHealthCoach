package com.aihealthcoach.meal.entity;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Food {
    private String foodCode;
    private String foodName;
    private String manufacturer;
    private BigDecimal servingSize;
    private String servingUnit;
    private BigDecimal calories;
    private BigDecimal carbohydrate;
    private BigDecimal protein;
    private BigDecimal fat;
}
