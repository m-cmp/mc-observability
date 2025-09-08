package com.mcmp.o11ymanager.trigger.application.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record TriggerVMDto(
        @NotNull @NotBlank String namespaceId,
        @NotNull @NotBlank String vmScope,
        @NotNull @NotBlank String vmId,
        boolean isActive) {}
