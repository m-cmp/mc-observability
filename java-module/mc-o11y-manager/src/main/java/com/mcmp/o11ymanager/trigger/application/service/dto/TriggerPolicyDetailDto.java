package com.mcmp.o11ymanager.trigger.application.service.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import com.mcmp.o11ymanager.trigger.application.common.dto.*;

@Builder
public record TriggerPolicyDetailDto(
        long id,
        String title,
        String description,
        ThresholdCondition thresholdCondition,
        String resourceType,
        String aggregationType,
        String holdDuration,
        String repeatInterval,
        List<TriggerTargetDetailDto> targets,
        List<TriggerPolicyNotiChannelDto> notiChannels,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
