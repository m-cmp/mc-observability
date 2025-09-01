package com.mcmp.o11ymanager.trigger.application.service.dto;

import com.mcmp.o11ymanager.trigger.application.common.type.AggregationType;
import com.mcmp.o11ymanager.trigger.application.common.type.ResourceType;
import lombok.Builder;
import com.mcmp.o11ymanager.trigger.application.common.dto.*;

@Builder
public record TriggerPolicyCreateDto(
        String title,
        String description,
        ThresholdCondition thresholdCondition,
        ResourceType resourceType,
        AggregationType aggregationType,
        String holdDuration,
        String repeatInterval) {}
