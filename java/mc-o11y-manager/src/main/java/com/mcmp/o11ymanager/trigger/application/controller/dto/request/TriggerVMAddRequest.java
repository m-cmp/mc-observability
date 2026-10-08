package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.TriggerVMDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TriggerVMAddRequest(
        @Schema(description = "Namespace ID", example = "first-ns") @NotNull @NotBlank String namespaceId,
        @Schema(description = "Target scope", example = "infra") @NotNull @NotBlank String targetScope,
        @Schema(description = "Target ID", example = "test01") @NotNull @NotBlank String targetId,
        @Schema(
                        description =
                                "Infra the node belongs to. Node IDs are only unique within an"
                                        + " infra, so without it a node target matches every infra"
                                        + " that has a node with this ID",
                        example = "infra-1")
                String infraId) {

    public TriggerVMDto toDto() {
        return new TriggerVMDto(namespaceId, targetScope, targetId, infraId, true);
    }
}
