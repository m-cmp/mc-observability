package com.mcmp.o11ymanager.exception.config;

import com.mcmp.o11ymanager.global.error.BaseException;
import com.mcmp.o11ymanager.global.error.ErrorCode;

public class FileReadingException extends BaseException {
    public FileReadingException(String message) {
        super(
                ErrorCode.FILE_READING.getCode(),
                message
        );
    }
}
