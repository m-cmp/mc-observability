package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerVMDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TriggerVMRemoveRequest(
        @Schema(description = "Namespace ID", example = "namespace-1")
                @NotNull @NotBlank String namespaceId,
        @Schema(description = "Target scope", example = "vm")
                @NotNull @NotBlank String targetScope,
        @Schema(description = "Target ID", example = "vm-1")
                @NotNull @NotBlank String targetId) {

    public TriggerVMDto toDto() {
        return new TriggerVMDto(namespaceId, targetScope, targetId, true);
    }
}
