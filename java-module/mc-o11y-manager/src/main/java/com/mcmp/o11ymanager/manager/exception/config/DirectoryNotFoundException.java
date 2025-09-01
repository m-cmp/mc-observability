package com.mcmp.o11ymanager.manager.exception.config;

import com.mcmp.o11ymanager.manager.global.error.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class DirectoryNotFoundException extends BaseException {
    public DirectoryNotFoundException(String message) {
        super(
                ErrorCode.DIRECTORY_NOT_FOUND.getCode(), message
        );
    }
}
