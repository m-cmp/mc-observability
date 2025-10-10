package com.mcmp.o11ymanager.manager.exception.host;

import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class HostNotExistException extends BaseException {

  public HostNotExistException(String requestId, String id) {
    super(requestId, ErrorCode.HOST_NOT_EXISTS, "Host does not exist: " + id);

  }
}
