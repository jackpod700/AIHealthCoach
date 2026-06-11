package com.aihealthcoach.chat.exception;

import com.aihealthcoach.common.error.BusinessException;

public class ChatException extends BusinessException {

    private ChatException(ChatErrorCode errorCode) {
        super(errorCode);
    }

    @Override
    public ChatErrorCode getErrorCode() {
        return (ChatErrorCode) super.getErrorCode();
    }

    public static ChatException imageRequired() {
        return new ChatException(ChatErrorCode.IMAGE_REQUIRED);
    }

    public static ChatException unsupportedImageType() {
        return new ChatException(ChatErrorCode.UNSUPPORTED_IMAGE_TYPE);
    }

    public static ChatException imageTooLarge() {
        return new ChatException(ChatErrorCode.IMAGE_TOO_LARGE);
    }

    public static ChatException imageTotalTooLarge() {
        return new ChatException(ChatErrorCode.IMAGE_TOTAL_TOO_LARGE);
    }
}
