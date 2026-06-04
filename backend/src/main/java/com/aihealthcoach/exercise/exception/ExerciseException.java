package com.aihealthcoach.exercise.exception;

import lombok.Getter;

@Getter
public class ExerciseException extends RuntimeException {

    private final ExerciseErrorCode errorCode;

    private ExerciseException(ExerciseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public static ExerciseException physicalActivityNotFound() {
        return new ExerciseException(ExerciseErrorCode.PHYSICAL_ACTIVITY_NOT_FOUND);
    }

    public static ExerciseException userWeightNotFound() {
        return new ExerciseException(ExerciseErrorCode.USER_WEIGHT_NOT_FOUND);
    }
}
