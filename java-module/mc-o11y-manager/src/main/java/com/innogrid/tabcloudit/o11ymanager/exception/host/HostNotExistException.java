package com.innogrid.tabcloudit.o11ymanager.exception.host;

import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class HostNotExistException extends BaseException {
    public HostNotExistException(String requestId, String id) {
        super(requestId, ErrorCode.HOST_NOT_EXISTS, "호스트가 존재하지 않습니다 : " + id);
    }
}
