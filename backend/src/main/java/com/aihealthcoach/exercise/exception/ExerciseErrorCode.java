package com.aihealthcoach.exercise.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExerciseErrorCode {
    PHYSICAL_ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "운동 마스터 데이터가 존재하지 않습니다."), 
    USER_WEIGHT_NOT_FOUND(HttpStatus.BAD_REQUEST, "소모 칼로리 계산에 필요한 현재 체중이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
