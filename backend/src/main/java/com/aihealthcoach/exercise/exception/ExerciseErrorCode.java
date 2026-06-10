package com.aihealthcoach.exercise.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import com.aihealthcoach.common.error.ErrorCode;

@Getter
@RequiredArgsConstructor
public enum ExerciseErrorCode implements ErrorCode{
    
    EXERCISE_ACTIVITY_OPTION_NOT_FOUND(HttpStatus.NOT_FOUND, "운동 선택지가 존재하지 않습니다."),
    EXERCISE_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "운동 기록이 존재하지 않습니다."),
    INVALID_INTENSITY_LEVEL(HttpStatus.BAD_REQUEST, "운동 강도는 LOW, MEDIUM, HIGH 중 하나여야 합니다."),
    USER_WEIGHT_NOT_FOUND(HttpStatus.BAD_REQUEST, "소모 칼로리 계산에 필요한 현재 체중이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
