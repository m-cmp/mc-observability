package com.mcmp.o11ymanager.trigger.application.service.dto;

import lombok.Builder;

@Builder
public record TriggerVMDetailDto(
        long id,
        String uuid,
        String namespaceId,
        String targetScope,
        String targetId,
        boolean isActive) {}
