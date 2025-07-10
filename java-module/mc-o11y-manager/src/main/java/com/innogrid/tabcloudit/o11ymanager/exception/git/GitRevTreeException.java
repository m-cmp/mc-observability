package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitRevTreeException extends BaseException {
    public GitRevTreeException() {
        super(null, ErrorCode.GIT_REV_TREE_NOT_FOUND, ErrorCode.GIT_REV_TREE_NOT_FOUND.getMessage());
    }
}
