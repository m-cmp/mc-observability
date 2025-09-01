package com.mcmp.o11ymanager.trigger.application.controller.dto.request;

import com.mcmp.o11ymanager.trigger.application.common.dto.ThresholdCondition;
import com.mcmp.o11ymanager.trigger.application.common.type.AggregationType;
import com.mcmp.o11ymanager.trigger.application.common.type.ResourceType;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyCreateDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Builder;

@Builder
public record TriggerPolicyCreateRequest(
        @NotNull @NotBlank String title,
        @NotNull @NotBlank String description,
        @NotNull @Valid ThresholdCondition thresholdCondition,
        @NotNull ResourceType resourceType,
        @NotNull AggregationType aggregationType,
        @NotNull @NotBlank @Pattern(
                        regexp = "^([0-9]|[1-5][0-9]|60)[sm]$",
                        message = "holdDuration must be in format: 0-60s or 1-60m")
                String holdDuration,
        @NotNull @NotBlank @Pattern(
                        regexp = "^([1-9]|1[0-9]|2[0-4])h$",
                        message = "repeatInterval must be in format: 1-24h")
                String repeatInterval) {

    public TriggerPolicyCreateDto toDto() {
        return TriggerPolicyCreateDto.builder()
                .title(title)
                .description(description)
                .thresholdCondition(thresholdCondition)
                .resourceType(resourceType)
                .aggregationType(aggregationType)
                .holdDuration(holdDuration)
                .repeatInterval(repeatInterval)
                .build();
    }
}
