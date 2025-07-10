package com.innogrid.tabcloudit.o11ymanager.exception.git;


import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitObjectIdNotFoundException extends BaseException {
    public GitObjectIdNotFoundException() {
        super(null, ErrorCode.GIT_OBJECT_ID_NOT_FOUND, ErrorCode.GIT_OBJECT_ID_NOT_FOUND.getMessage());
    }
}
