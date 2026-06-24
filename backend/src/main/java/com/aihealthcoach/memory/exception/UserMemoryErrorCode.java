package com.aihealthcoach.memory.exception;

import com.aihealthcoach.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserMemoryErrorCode implements ErrorCode {

    USER_MEMORY_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 memory를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;
}
