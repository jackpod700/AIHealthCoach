package com.aihealthcoach.common.fatsecret;

import com.aihealthcoach.common.error.BusinessException;

public class FatSecretException extends BusinessException {

    private FatSecretException(FatSecretErrorCode errorCode) {
        super(errorCode);
    }

    private FatSecretException(FatSecretErrorCode errorCode, String detail) {
        super(errorCode, withDetail(errorCode, detail));
    }

    public static FatSecretException configMissing() {
        return new FatSecretException(FatSecretErrorCode.FATSECRET_CONFIG_MISSING);
    }

    public static FatSecretException tokenRequestFailed() {
        return new FatSecretException(FatSecretErrorCode.FATSECRET_TOKEN_REQUEST_FAILED);
    }

    public static FatSecretException tokenRequestFailed(String detail) {
        return new FatSecretException(FatSecretErrorCode.FATSECRET_TOKEN_REQUEST_FAILED, detail);
    }

    public static FatSecretException searchRequestFailed() {
        return new FatSecretException(FatSecretErrorCode.FATSECRET_SEARCH_REQUEST_FAILED);
    }

    public static FatSecretException searchRequestFailed(String detail) {
        return new FatSecretException(FatSecretErrorCode.FATSECRET_SEARCH_REQUEST_FAILED, detail);
    }

    public static FatSecretException responseInvalid() {
        return new FatSecretException(FatSecretErrorCode.FATSECRET_RESPONSE_INVALID);
    }

    public static FatSecretException responseInvalid(String detail) {
        return new FatSecretException(FatSecretErrorCode.FATSECRET_RESPONSE_INVALID, detail);
    }

    private static String withDetail(FatSecretErrorCode errorCode, String detail) {
        if (detail == null || detail.isBlank()) {
            return errorCode.getMessage();
        }

        return errorCode.getMessage() + " " + detail;
    }
}
