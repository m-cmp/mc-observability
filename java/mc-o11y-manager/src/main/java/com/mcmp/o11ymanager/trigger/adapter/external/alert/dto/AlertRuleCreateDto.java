package com.mcmp.o11ymanager.trigger.adapter.external.alert.dto;

import com.mcmp.o11ymanager.trigger.application.common.type.ResourceType;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerPolicyDetailDto;
import com.mcmp.o11ymanager.trigger.application.service.dto.TriggerVMDetailDto;
import lombok.Builder;

@Builder
public record AlertRuleCreateDto(
        String uuid,
        ResourceType resourceType,
        String targetScope,
        String measurement,
        String aggregation,
        String field,
        String targetId,
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
                .targetScope(triggerVMDto.targetScope())
                .namespaceId(triggerVMDto.namespaceId())
                .targetId(triggerVMDto.targetId())
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
            String targetId,
            String namespaceId,
            String title,
            String holdDuration,
            String thresholdExpression) {
        return AlertRuleCreateDto.builder()
                .uuid(uuid)
                .measurement(measurement)
                .aggregation(aggregation)
                .field(field)
                .targetId(targetId)
                .namespaceId(namespaceId)
                .title(title)
                .holdDuration(holdDuration)
                .thresholdExpression(thresholdExpression)
                .build();
    }
}
