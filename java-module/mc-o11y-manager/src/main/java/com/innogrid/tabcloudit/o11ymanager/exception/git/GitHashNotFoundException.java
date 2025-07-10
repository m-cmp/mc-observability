package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitHashNotFoundException extends BaseException {
    public GitHashNotFoundException() {
        super(null, ErrorCode.GIT_HASH_NOT_FOUND, ErrorCode.GIT_HASH_NOT_FOUND.getMessage());
    }
}
