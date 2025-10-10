package com.mcmp.o11ymanager.manager.exception.config;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import com.mcmp.o11ymanager.manager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class ConfigNotFoundException extends BaseException {

  private String filePath;

  public ConfigNotFoundException(String requestId, String filePath) {
    super(requestId, ErrorCode.CONFIG_NOT_FOUND, "Configuration file not found: " + filePath);

  }
}
