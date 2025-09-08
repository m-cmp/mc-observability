package com.mcmp.o11ymanager.trigger.adapter.external.alert.dto;

import com.mcmp.o11ymanager.trigger.application.common.type.ResourceType;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerVMDetailDto;
import lombok.Builder;

@Builder
public record AlertRuleCreateDto(
        String uuid,
        ResourceType resourceType,
        String vmScope,
        String measurement,
        String aggregation,
        String field,
        String vmId,
        String namespaceId,
        String title,
        String holdDuration,
        String repeatInterval,
        String thresholdExpression) {

    public static AlertRuleCreateDto from(
            TriggerPolicyDetailDto triggerPolicyDto, TriggerVMDetailDto triggerVMDto) {
        ResourceType resourceType =
                ResourceType.findBy(triggerPolicyDto.resourceType().toLowerCase());

        return AlertRuleCreateDto.builder()
                .uuid(triggerVMDto.uuid())
                .resourceType(resourceType)
                .vmScope(triggerVMDto.vmScope())
                .namespaceId(triggerVMDto.namespaceId())
                .vmId(triggerVMDto.vmId())
                .title(triggerPolicyDto.title())
                .measurement(resourceType.getMeasurement())
                .field(resourceType.getField())
                .aggregation(triggerPolicyDto.aggregationType())
                .holdDuration(triggerPolicyDto.holdDuration())
                .repeatInterval(triggerPolicyDto.repeatInterval())
                .thresholdExpression(triggerPolicyDto.thresholdCondition().toJson())
                .build();
    }

    public static AlertRuleCreateDto create(
            String uuid,
            String measurement,
            String aggregation,
            String field,
            String vmId,
            String namespaceId,
            String title,
            String holdDuration,
            String thresholdExpression) {
        return AlertRuleCreateDto.builder()
                .uuid(uuid)
                .measurement(measurement)
                .aggregation(aggregation)
                .field(field)
                .vmId(vmId)
                .namespaceId(namespaceId)
                .title(title)
                .holdDuration(holdDuration)
                .thresholdExpression(thresholdExpression)
                .build();
    }
}
