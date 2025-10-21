package com.mcmp.o11ymanager.trigger.application.common.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcmp.o11ymanager.trigger.application.common.exception.InvalidThresholdConditionException;
import jakarta.validation.constraints.Min;
import lombok.Builder;

@Builder
public record ThresholdCondition(
        @Min(value = 0, message = "info threshold must be non-negative") double info,
        @Min(value = 0, message = "warning threshold must be non-negative") double warning,
        @Min(value = 0, message = "critical threshold must be non-negative") double critical) {

    public static ThresholdCondition from(String json) {
        try {
            return new ObjectMapper().readValue(json, ThresholdCondition.class);
        } catch (JsonProcessingException e) {
            throw new InvalidThresholdConditionException("Failed to parse JSON: " + json, e);
        }
    }

    public String toJson() {
        return """
				{
				    "info": "%s",
				    "warning": "%s",
				    "critical": "%s"
				}
				"""
                .formatted(info, warning, critical);
    }
}
