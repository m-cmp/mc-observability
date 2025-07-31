package com.mcmp.o11ymanager.exception;

import com.mcmp.o11ymanager.enums.ResponseCode;
import lombok.Getter;

@Getter
public class TelegrafConfigException extends RuntimeException {
    private final ResponseCode responseCode;
    
    public TelegrafConfigException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }
}