package com.aihealthcoach.dailygoal.exception;

import com.aihealthcoach.common.error.BusinessException;

public class DailyGoalException extends BusinessException {

    private DailyGoalException(DailyGoalErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public DailyGoalErrorCode getErrorCode() {
        return (DailyGoalErrorCode) super.getErrorCode();
    }

    public static DailyGoalException dailyGoalNotFound() {
        return new DailyGoalException(DailyGoalErrorCode.DAILY_GOAL_NOT_FOUND);
    }

    public static DailyGoalException invalidGoalType() {
        return new DailyGoalException(DailyGoalErrorCode.INVALID_GOAL_TYPE);
    }

    public static DailyGoalException profileRequired() {
        return new DailyGoalException(DailyGoalErrorCode.PROFILE_REQUIRED);
    }
}
