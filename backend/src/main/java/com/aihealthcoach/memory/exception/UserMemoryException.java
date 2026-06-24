package com.aihealthcoach.memory.exception;

import com.aihealthcoach.common.error.BusinessException;

public class UserMemoryException extends BusinessException {

    private UserMemoryException(UserMemoryErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public UserMemoryErrorCode getErrorCode() {
        return (UserMemoryErrorCode) super.getErrorCode();
    }

    public static UserMemoryException userMemoryNotFound() {
        return new UserMemoryException(UserMemoryErrorCode.USER_MEMORY_NOT_FOUND);
    }
}
