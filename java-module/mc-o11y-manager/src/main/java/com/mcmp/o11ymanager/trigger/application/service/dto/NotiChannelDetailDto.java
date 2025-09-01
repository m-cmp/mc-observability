package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record NotiChannelDetailDto(
        long id,
        String name,
        String type,
        String provider,
        String baseUrl,
        boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
