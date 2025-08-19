package com.mcmp.o11ymanager.dto.target;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.enums.AgentServiceStatus;
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

  private String description;

  @JsonProperty("influx_seq")
  private int influxSeq;

  @JsonProperty("target_status")
  private TargetStatus targetStatus;

  @JsonProperty("monitoring_agent_task_status")
  @JsonIgnore
  private TargetAgentTaskStatus monitoringAgentTaskStatus;

  @JsonProperty("log_agent_task_status")
  @JsonIgnore
  private TargetAgentTaskStatus logAgentTaskStatus;

  @JsonProperty("target_monitoring_agent_task_id")
  @JsonIgnore
  private String targetMonitoringAgentTaskId;

  @JsonProperty("target_log_agent_task_id")
  @JsonIgnore
  private String targetLogAgentTaskId;

  @JsonProperty("monitoring_service_status")
  private AgentServiceStatus monitoringServiceStatus;

  @JsonProperty("log_service_status")
  private AgentServiceStatus logServiceStatus;

  @JsonProperty("ns_id")
  private String nsId;

  @JsonProperty("mci_id")
  private String mciId;



  public static TargetDTO fromEntity(com.mcmp.o11ymanager.entity.TargetEntity entity) {
    return TargetDTO.builder()
        .targetId(entity.getTargetId())
        .name(entity.getName())
        .description(entity.getDescription())
        .influxSeq(entity.getInfluxSeq())
        .targetStatus(entity.getTargetStatus())
        .description(entity.getDescription())
        .targetMonitoringAgentTaskId(entity.getTargetMonitoringAgentTaskId())
        .targetLogAgentTaskId(entity.getTargetLogAgentTaskId())
        .monitoringServiceStatus(entity.getMonitoringServiceStatus())
        .logServiceStatus(entity.getLogServiceStatus())
        .nsId(entity.getNsId())
        .mciId(entity.getMciId())
        .build();
  }

  public TargetEntity toEntity() {
    return TargetEntity.builder()
        .targetId(this.getTargetId())
        .name(this.name)
        .influxSeq(this.getInfluxSeq())
        .targetStatus(this.getTargetStatus())
        .description(this.description)
        .monitoringAgentTaskStatus(this.monitoringAgentTaskStatus)
        .logAgentTaskStatus(this.logAgentTaskStatus)
        .targetMonitoringAgentTaskId(this.targetMonitoringAgentTaskId)
        .targetLogAgentTaskId(this.targetLogAgentTaskId)
        .monitoringServiceStatus(this.monitoringServiceStatus)
        .logServiceStatus(this.logServiceStatus)
        .nsId(this.nsId)
        .mciId(this.mciId)
        .build();
  }
}