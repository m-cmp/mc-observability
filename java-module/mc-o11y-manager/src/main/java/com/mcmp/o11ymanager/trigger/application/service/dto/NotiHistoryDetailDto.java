package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;

@Builder
public record NotiHistoryDetailDto(
        long id,
        String channel,
        List<String> recipients,
        String exception,
        boolean isSucceeded,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
