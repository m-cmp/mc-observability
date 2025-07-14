package com.mcmp.o11ymanager.exception.agent;

import com.mcmp.o11ymanager.exception.host.BaseException;
import com.mcmp.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class SshConnectionException extends BaseException {
    public SshConnectionException(String requestId, String ip) {
        super(
                requestId,
                ErrorCode.SSH_CONNECTION_FAILED,
                String.format("Host(IP: %s) 접속 정보 실패.", ip)
        );
    }
}
