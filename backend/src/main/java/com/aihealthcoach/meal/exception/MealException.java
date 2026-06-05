package com.aihealthcoach.meal.exception;

import com.aihealthcoach.common.error.BusinessException;

public class MealException extends BusinessException {

    private MealException(MealErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public MealErrorCode getErrorCode() {
        return (MealErrorCode) super.getErrorCode();
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
