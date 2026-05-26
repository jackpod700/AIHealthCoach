package com.aihealthcoach.user.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode {

    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "가입되지 않은 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 프로필이 존재하지 않는 userId입니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "올바르지 않은 JWT 토큰입니다."),
    PROFILE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "프로필에 접근이 불가능합니다.");

    private final HttpStatus status;
    private final String message;
}
