package com.innogrid.tabcloudit.o11ymanager.exception.git;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;

public class GitTreeWalkException extends BaseException {
    public GitTreeWalkException() {
      super(null, ErrorCode.GIT_TREE_WALK_FAILURE, ErrorCode.GIT_TREE_WALK_FAILURE.getMessage());

    }
}
