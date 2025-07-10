package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitRevertException extends BaseException {
    public GitRevertException() {
        super(null, ErrorCode.GIT_REVERT_FAILURE, ErrorCode.GIT_REVERT_FAILURE.getMessage());
    }
}
