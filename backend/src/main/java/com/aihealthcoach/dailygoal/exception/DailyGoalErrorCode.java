package com.aihealthcoach.dailygoal.exception;

import com.aihealthcoach.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DailyGoalErrorCode implements ErrorCode {

    DAILY_GOAL_NOT_FOUND(HttpStatus.NOT_FOUND, "일일 목표가 설정되지 않았습니다."),
    INVALID_GOAL_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 목표 유형입니다."),
    PROFILE_REQUIRED(HttpStatus.BAD_REQUEST, "목표 추천에 필요한 프로필 정보가 부족합니다.");

    private final HttpStatus status;
    private final String message;
}
