package com.aihealthcoach.meal.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealErrorCode {

    INVALID_MEAL_TYPE(HttpStatus.BAD_REQUEST, "Invalid meal type."),
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "Food does not exist."),
    INVALID_MEAL_ITEM(HttpStatus.BAD_REQUEST, "Meal item is invalid.");

    private final HttpStatus status;
    private final String message;
}
