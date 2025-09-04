package com.mcmp.o11ymanager.manager.exception.host;

import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class HostNotExistException extends BaseException {
    public HostNotExistException(String requestId, String id) {
        super(requestId, ErrorCode.HOST_NOT_EXISTS, "호스트가 존재하지 않습니다 : " + id);
    }
}
