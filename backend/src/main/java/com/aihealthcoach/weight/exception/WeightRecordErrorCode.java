package com.aihealthcoach.weight.exception;

import com.aihealthcoach.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WeightRecordErrorCode implements ErrorCode {

    WEIGHT_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "몸무게 기록이 존재하지 않습니다."),
    INVALID_WEIGHT(HttpStatus.BAD_REQUEST, "몸무게 값이 올바르지 않습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "몸무게 기록 조회 기간이 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
