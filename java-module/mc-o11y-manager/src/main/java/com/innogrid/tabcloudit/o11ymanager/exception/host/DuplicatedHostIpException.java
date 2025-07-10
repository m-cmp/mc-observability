package com.innogrid.tabcloudit.o11ymanager.exception.host;

import com.innogrid.tabcloudit.o11ymanager.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class DuplicatedHostIpException extends BaseException {
    public DuplicatedHostIpException(String requestId, String ipPortList) {
        super(requestId, ErrorCode.DUPLICATE_HOST, "중복된 호스트 "+ ipPortList + "가 존재합니다.");
    }
}
