package com.aihealthcoach.meal.exception;

import org.springframework.http.HttpStatus;

import com.aihealthcoach.common.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MealErrorCode implements ErrorCode {

    INVALID_MEAL_TYPE(HttpStatus.BAD_REQUEST, "Invalid meal type."),
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "Food does not exist."),
    INVALID_MEAL_ITEM(HttpStatus.BAD_REQUEST, "Meal item is invalid."),
    MEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "Meal does not exist."),
    INVALID_FOOD_SUBMISSION(HttpStatus.BAD_REQUEST, "Food submission request is invalid."),
    FOOD_SUBMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "Food submission request does not exist."),
    FOOD_SUBMISSION_ALREADY_REVIEWED(HttpStatus.BAD_REQUEST, "Food submission request was already reviewed.");

    private final HttpStatus status;
    private final String message;
}
