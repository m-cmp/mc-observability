package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerVMDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerVMUpdateDto;
import java.util.List;

public record TriggerVMUpdateRequest(List<TriggerVMDto> triggerVMs) {

    public TriggerVMUpdateDto toDto() {
        return new TriggerVMUpdateDto(triggerVMs);
    }
}
