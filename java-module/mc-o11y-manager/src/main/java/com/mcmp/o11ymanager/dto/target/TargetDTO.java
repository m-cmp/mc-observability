package com.mcmp.o11ymanager.dto.target;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetDTO {

    @JsonProperty("target_id")
    private String targetId;

    private String name;

    @JsonProperty("alias_name")
    private String aliasName;

    private String description;

    @JsonProperty("target_status")
    private TargetStatus targetStatus;

    @JsonProperty("monitoring_agent_task_status")
    private TargetAgentTaskStatus monitoringAgentTaskStatus;

    @JsonProperty("log_agent_task_status")
    private TargetAgentTaskStatus logAgentTaskStatus;

    @JsonProperty("target_monitoring_agent_task_id")
    private String targetMonitoringAgentTaskId;

    @JsonProperty("target_log_agent_task_id")
    private String targetLogAgentTaskId;

    @JsonProperty("monitoring_service_status")
    private String monitoringServiceStatus;

    @JsonProperty("log_service_status")
    private String logServiceStatus;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("ns_id")
    private String nsId;

    @JsonProperty("mci_id")
    private String mciId;

    private String state;

    public static TargetDTO fromEntity(com.mcmp.o11ymanager.entity.TargetEntity entity) {
        return TargetDTO.builder()
                .targetId(entity.getTargetId())
                .name(entity.getName())
                .aliasName(entity.getAliasName())
                .description(entity.getDescription())
                .targetStatus(entity.getTargetStatus())
                .targetMonitoringAgentTaskId(entity.getTargetMonitoringAgentTaskId())
                .targetLogAgentTaskId(entity.getTargetLogAgentTaskId())
                .monitoringServiceStatus(entity.getMonitoringServiceStatus())
                .logServiceStatus(entity.getLogServiceStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .nsId(entity.getNsId())
                .mciId(entity.getMciId())
                .state(entity.getState())
                .build();
    }

    public TargetEntity toEntity() {
        return TargetEntity.builder()
                .targetId(this.getTargetId())
                .name(this.name)
                .aliasName(this.aliasName)
                .description(this.description)
                .targetStatus(this.getTargetStatus())
                .monitoringAgentTaskStatus(this.monitoringAgentTaskStatus)
                .logAgentTaskStatus(this.logAgentTaskStatus)
                .targetMonitoringAgentTaskId(this.targetMonitoringAgentTaskId)
                .targetLogAgentTaskId(this.targetLogAgentTaskId)
                .monitoringServiceStatus(this.monitoringServiceStatus)
                .logServiceStatus(this.logServiceStatus)
                .createdAt(this.createdAt)
                .updatedAt(this.updatedAt)
                .nsId(this.nsId)
                .mciId(this.mciId)
                .state(this.state)
                .build();
    }
}