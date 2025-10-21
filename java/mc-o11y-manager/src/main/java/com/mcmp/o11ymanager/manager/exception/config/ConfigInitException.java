package com.mcmp.o11ymanager.manager.exception.config;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class ConfigInitException extends BaseException {

    public ConfigInitException(String requestId, String path) {
        super(
                requestId,
                ErrorCode.CONFIG_INIT_FAILURE,
                String.format("Failed to create config base directory. Path: %s", path));
    }
}
