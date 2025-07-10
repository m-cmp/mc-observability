package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitFileOpenException extends BaseException {

    public GitFileOpenException() {
        super(null, ErrorCode.GIT_FILE_NOT_FOUND, ErrorCode.GIT_FILE_NOT_FOUND.getMessage());
    }
}
