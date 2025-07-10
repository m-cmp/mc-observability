package com.innogrid.tabcloudit.o11ymanager.global.error;

import lombok.Getter;

@Getter
public abstract class BaseException extends RuntimeException {

    private final String requestId;

    public BaseException(String requestId, String message) {
        super(message);
        this.requestId = requestId;
    }
}
