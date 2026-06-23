package com.aihealthcoach.common.fatsecret;

import org.springframework.http.HttpStatus;

import com.aihealthcoach.common.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FatSecretErrorCode implements ErrorCode {

    FATSECRET_CONFIG_MISSING(HttpStatus.BAD_REQUEST, "FatSecret credentials are not configured."),
    FATSECRET_TOKEN_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "FatSecret token request failed."),
    FATSECRET_SEARCH_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "FatSecret food search request failed."),
    FATSECRET_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY, "FatSecret response is invalid.");

    private final HttpStatus status;
    private final String message;
}
