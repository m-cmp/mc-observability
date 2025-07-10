package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitCommitContentsException extends BaseException {
    public GitCommitContentsException() {
        super(null, ErrorCode.GIT_COMMIT_CONTENT_NOT_FOUND, ErrorCode.GIT_COMMIT_CONTENT_NOT_FOUND.getMessage());
    }
}
