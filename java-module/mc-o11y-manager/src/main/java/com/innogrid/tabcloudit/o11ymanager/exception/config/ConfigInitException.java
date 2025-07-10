package com.innogrid.tabcloudit.o11ymanager.exception.config;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class ConfigInitException extends BaseException {
  public ConfigInitException(String requestId, String path) {
    super(
            requestId,
            ErrorCode.CONFIG_INIT_FAILURE,
            String.format("Config 베이스 디렉토리 생성에 실패했습니다. 경로: %s", path)
    );
  }
}
