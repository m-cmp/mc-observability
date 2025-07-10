package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitHistoryNotFoundException extends BaseException {
    public GitHistoryNotFoundException() {
        super(null, ErrorCode.GIT_HISTORY_NOT_FOUND, ErrorCode.GIT_HISTORY_NOT_FOUND.getMessage());
    }
}
