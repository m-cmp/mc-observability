package com.mcmp.o11ymanager.manager.exception.config;

import com.mcmp.o11ymanager.manager.global.error.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class FailedDeleteFileException extends BaseException {
    public FailedDeleteFileException(String message) {
        super(ErrorCode.FILE_READING.getCode(), message);
    }
}
