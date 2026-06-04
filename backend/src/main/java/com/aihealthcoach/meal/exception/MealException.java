package com.aihealthcoach.meal.exception;

import lombok.Getter;

@Getter
public class MealException extends RuntimeException {

    private final MealErrorCode errorCode;

    private MealException(MealErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public static MealException invalidMealType() {
        return new MealException(MealErrorCode.INVALID_MEAL_TYPE);
    }

    public static MealException foodNotFound() {
        return new MealException(MealErrorCode.FOOD_NOT_FOUND);
    }

    public static MealException invalidMealItem() {
        return new MealException(MealErrorCode.INVALID_MEAL_ITEM);
    }

    public static MealException mealNotFound() {
        return new MealException(MealErrorCode.MEAL_NOT_FOUND);
    }
}
