package com.aihealthcoach.weight.exception;

import com.aihealthcoach.common.error.BusinessException;

public class WeightRecordException extends BusinessException {

    private WeightRecordException(WeightRecordErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public WeightRecordErrorCode getErrorCode() {
        return (WeightRecordErrorCode) super.getErrorCode();
    }

    public static WeightRecordException weightRecordNotFound() {
        return new WeightRecordException(WeightRecordErrorCode.WEIGHT_RECORD_NOT_FOUND);
    }

    public static WeightRecordException invalidWeight() {
        return new WeightRecordException(WeightRecordErrorCode.INVALID_WEIGHT);
    }

    public static WeightRecordException invalidDateRange() {
        return new WeightRecordException(WeightRecordErrorCode.INVALID_DATE_RANGE);
    }
}
