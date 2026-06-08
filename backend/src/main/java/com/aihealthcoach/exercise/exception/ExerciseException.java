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

    public static ExerciseException exerciseActivityOptionNotFound() {
        return new ExerciseException(ExerciseErrorCode.EXERCISE_ACTIVITY_OPTION_NOT_FOUND);
    }

    public static ExerciseException exerciseRecordNotFound() {
        return new ExerciseException(ExerciseErrorCode.EXERCISE_RECORD_NOT_FOUND);
    }

    public static ExerciseException invalidIntensityLevel() {
        return new ExerciseException(ExerciseErrorCode.INVALID_INTENSITY_LEVEL);
    }

    public static ExerciseException userWeightNotFound() {
        return new ExerciseException(ExerciseErrorCode.USER_WEIGHT_NOT_FOUND);
    }
}
