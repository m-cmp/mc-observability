package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TriggerHistoryDetailDto(
        long id,
        String triggerTitle,
        String aggregationType,
        String holdDuration,
        String repeatInterval,
        String resourceType,
        String namespaceId,
        String mciId,
        String vmId,
        String threshold,
        String resourceUsage,
        String alertLevel,
        String status,
        String comment,
        LocalDateTime startsAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
