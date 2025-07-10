package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;


public class GitInitException extends BaseException {
    public GitInitException() {
        super(null, ErrorCode.GIT_INIT_FAILURE, ErrorCode.GIT_INIT_FAILURE.getMessage());
    }

}
