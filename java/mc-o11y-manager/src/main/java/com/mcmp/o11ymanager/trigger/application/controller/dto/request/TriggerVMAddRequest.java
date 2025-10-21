package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerVMDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TriggerVMAddRequest(
        @NotNull @NotBlank String namespaceId,
        @NotNull @NotBlank String targetScope,
        @NotNull @NotBlank String targetId) {

    public TriggerVMDto toDto() {
        return new TriggerVMDto(namespaceId, targetScope, targetId, true);
    }
}
