package com.mcmp.o11ymanager.trigger.application.controller.dto.response;

import lombok.Builder;

@Builder
public record GrafanaHealthCheckResponse(
        HealthStatus contactPoint, HealthStatus datasource, HealthStatus folder, HealthStatus org) {
    public record HealthStatus(boolean hasData) {}
}
