package com.innogrid.tabcloudit.o11ymanager.exception.config;

import com.innogrid.tabcloudit.o11ymanager.global.error.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class FailedDeleteFileException extends BaseException {
    public FailedDeleteFileException(String message) {
        super(
                ErrorCode.FILE_READING.getCode(),
                message
        );
    }
}
