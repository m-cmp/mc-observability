package com.mcmp.o11ymanager.manager.global.error;

import com.mcmp.o11ymanager.manager.exception.host.BaseException;
import lombok.Getter;

@Getter
public class ResourceNotExistsException extends BaseException {

    public ResourceNotExistsException(String requestId, String resourceName, String id) {
        super(requestId, ErrorCode.RESOURCE_NOT_EXISTS, resourceName + " ID does not exist: " + id);
    }
}
