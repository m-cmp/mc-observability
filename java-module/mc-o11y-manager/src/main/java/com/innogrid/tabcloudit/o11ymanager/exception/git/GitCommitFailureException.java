package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitCommitFailureException extends BaseException {
    public GitCommitFailureException() {
        super(null, ErrorCode.GIT_COMMIT_FAILURE, ErrorCode.GIT_COMMIT_FAILURE.getMessage());
    }
}
