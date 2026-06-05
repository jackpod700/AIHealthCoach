package com.aihealthcoach.exercise.exception;

import com.aihealthcoach.common.error.BusinessException;

public class ExerciseException extends BusinessException {

    private ExerciseException(ExerciseErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public ExerciseErrorCode getErrorCode() {
        return (ExerciseErrorCode) super.getErrorCode();
    }

    public static ExerciseException physicalActivityNotFound() {
        return new ExerciseException(ExerciseErrorCode.PHYSICAL_ACTIVITY_NOT_FOUND);
    }

    public static ExerciseException userWeightNotFound() {
        return new ExerciseException(ExerciseErrorCode.USER_WEIGHT_NOT_FOUND);
    }
}
