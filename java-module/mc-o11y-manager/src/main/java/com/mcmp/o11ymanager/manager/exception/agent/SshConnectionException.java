package com.mcmp.o11ymanager.manager.exception.agent;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class SshConnectionException extends BaseException {

  public SshConnectionException(String requestId, String ip) {
    super(
        requestId,
        ErrorCode.SSH_CONNECTION_FAILED,
        String.format("Failed to retrieve connection info for host (IP: %s).", ip));

  }
}
