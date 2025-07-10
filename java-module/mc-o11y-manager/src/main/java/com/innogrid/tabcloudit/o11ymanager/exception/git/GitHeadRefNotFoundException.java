package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitHeadRefNotFoundException extends BaseException {
    public GitHeadRefNotFoundException() {
        super(null, ErrorCode.GIT_HEAD_REF_NOT_FOUND, ErrorCode.GIT_HEAD_REF_NOT_FOUND.getMessage());
    }
}
