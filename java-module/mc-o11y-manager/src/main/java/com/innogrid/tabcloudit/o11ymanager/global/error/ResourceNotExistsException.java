package com.innogrid.tabcloudit.o11ymanager.global.error;

import com.innogrid.tabcloudit.o11ymanager.exception.host.BaseException;
import lombok.Getter;

@Getter
public class ResourceNotExistsException extends BaseException {

    public ResourceNotExistsException(String requestId, String resourceName, String id) {
        super(requestId, ErrorCode.RESOURCE_NOT_EXISTS, resourceName + " ID가 존재하지 않습니다 : " + id);
    }

}
