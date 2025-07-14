package com.mcmp.o11ymanager.exception.host;

import com.mcmp.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final String requestId;
    private final ErrorCode errorCode;

    public BaseException(String requestId, ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.requestId = requestId;
        this.errorCode = errorCode;
    }

    public BaseException(String requestId, ErrorCode errorCode, String message) {
        super(message);
        this.requestId = requestId;
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return String.format("ErrorCode: %s, Message: %s", errorCode.getCode(), super.getMessage());
    }
}
