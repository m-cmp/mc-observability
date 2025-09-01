package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerTargetDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TriggerTargetAddRequest(
        @NotNull @NotBlank String namespaceId,
        @NotNull @NotBlank String targetScope,
        @NotNull @NotBlank String targetId) {

    public TriggerTargetDto toDto() {
        return new TriggerTargetDto(namespaceId, targetScope, targetId, true);
    }
}
