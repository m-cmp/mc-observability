package com.innogrid.tabcloudit.o11ymanager.exception.host;

import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class HostConnectionFailedException extends BaseException {
    public HostConnectionFailedException(String requestId, String id) {
        super(requestId, ErrorCode.HOST_CONNECTION_FAILED, "호스트에 연결할 수 없습니다 : " + id);
    }
}
