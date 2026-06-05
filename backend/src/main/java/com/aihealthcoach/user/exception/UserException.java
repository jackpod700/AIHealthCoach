package com.aihealthcoach.user.exception;

import com.aihealthcoach.common.error.BusinessException;

public class UserException extends BusinessException {

    private UserException(UserErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public UserErrorCode getErrorCode() {
        return (UserErrorCode) super.getErrorCode();
    }

    public static UserException duplicateEmail() {
        return new UserException(UserErrorCode.DUPLICATE_EMAIL);
    }

    public static UserException userNotFound() {
        return new UserException(UserErrorCode.USER_NOT_FOUND);
    }

    public static UserException invalidPassword() {
        return new UserException(UserErrorCode.INVALID_PASSWORD);
    }

    public static UserException profileNotFound() {
        return new UserException(UserErrorCode.PROFILE_NOT_FOUND);
    }

    public static UserException invalidToken(){
        return new UserException(UserErrorCode.INVALID_TOKEN);
    }

    public static UserException profileAccessDenied(){
        return new UserException(UserErrorCode.PROFILE_ACCESS_DENIED);
    }
}
