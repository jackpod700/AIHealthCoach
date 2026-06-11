package com.aihealthcoach.chat.exception;

import org.springframework.http.HttpStatus;

import com.aihealthcoach.common.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {

    IMAGE_REQUIRED(HttpStatus.BAD_REQUEST, "At least one image is required."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, and WebP images are supported."),
    IMAGE_TOO_LARGE(HttpStatus.BAD_REQUEST, "Each image must be 10KB or smaller."),
    IMAGE_TOTAL_TOO_LARGE(HttpStatus.BAD_REQUEST, "Total image size must be 50KB or smaller.");

    private final HttpStatus status;
    private final String message;
}
