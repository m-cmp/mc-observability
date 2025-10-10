package com.mcmp.o11ymanager.manager.exception.host;

import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicatedHostIpException extends BaseException {

  public DuplicatedHostIpException(String requestId, String ipPortList) {
    super(requestId, ErrorCode.DUPLICATE_HOST, "Duplicate host(s) found: " + ipPortList);

  }
}
