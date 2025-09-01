package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AlertTestHistoryDetailDto(
        Long id, String message, LocalDateTime createdAt, LocalDateTime updatedAt) {}
