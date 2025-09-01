package com.mcmp.o11ymanager.manager.exception.config;

import com.mcmp.o11ymanager.manager.enums.ResponseCode;
import lombok.Getter;

@Getter
public class TelegrafConfigException extends RuntimeException {
    private final ResponseCode responseCode;
    
    public TelegrafConfigException(ResponseCode responseCode, String message) {
        super(message);
        this.responseCode = responseCode;
    }
}