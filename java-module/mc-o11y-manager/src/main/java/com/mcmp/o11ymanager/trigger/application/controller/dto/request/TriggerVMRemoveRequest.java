package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerVMDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TriggerVMRemoveRequest(
        @NotNull @NotBlank String namespaceId,
        @NotNull @NotBlank String vmScope,
        @NotNull @NotBlank String vmId) {

    public TriggerVMDto toDto() {
        return new TriggerVMDto(namespaceId, vmScope, vmId, true);
    }
}
