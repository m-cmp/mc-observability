package com.mcmp.o11ymanager.dto.host;

import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.model.host.HostAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.HostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "호스트 정보를 위한 DTO")
public class HostDTO {

  private String id;
  private String name;
  private String hostname;
  private String description;
  private String credentialId;
  private String cloudService;
  private String type;
  private String ip;
  private int port;
  private String user;
  private String password;
  private HostStatus host_status;
  private HostAgentTaskStatus host_monitoring_agent_task_status;
  private HostAgentTaskStatus host_log_agent_task_status;
  private String host_monitoring_agent_task_id;
  private String host_log_agent_task_id;
  private String monitoringServiceStatus;
  private String logServiceStatus;
  private String monitoring_agent_config_git_hash;
  private String log_agent_config_git_hash;
  private String monitoring_agent_version;
  private String log_agent_version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // HostEntity와 DTO 간의 변환 메서드
  public static HostDTO fromEntity(HostEntity entity) {
    return HostDTO.builder()
        .id(entity.getId())
        .name(entity.getName())
        .hostname(entity.getHostname())
        .description(entity.getDescription())
        .credentialId(entity.getCredential_id())
        .cloudService(entity.getCloud_service())
        .type(entity.getType())
        .ip(entity.getIp())
        .port(entity.getPort())
        .user(entity.getUser())
        .password(entity.getPassword())
        .host_status(entity.getHost_status())
        .host_monitoring_agent_task_status(entity.getHost_monitoring_agent_task_status())
        .host_log_agent_task_status(entity.getHost_log_agent_task_status())
        .host_monitoring_agent_task_id(entity.getHost_monitoring_agent_task_id())
        .host_log_agent_task_id(entity.getHost_log_agent_task_id())
        .monitoringServiceStatus(entity.getMonitoringServiceStatus())
        .logServiceStatus(entity.getLogServiceStatus())
        .monitoring_agent_config_git_hash(entity.getMonitoring_agent_config_git_hash())
        .log_agent_config_git_hash(entity.getLog_agent_config_git_hash())
        .monitoring_agent_version(entity.getMonitoring_agent_version())
        .log_agent_version(entity.getLog_agent_version())
        .createdAt(entity.getCreatedAt())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  public HostEntity toEntity() {
    return HostEntity.builder()
        .id(this.id)
        .name(this.name)
        .credential_id(this.credentialId)
        .cloud_service(this.cloudService)
        .hostname(this.hostname)
        .description(this.description)
        .type(this.type)
        .ip(this.ip)
        .port(this.port)
        .user(this.user)
        .password(this.password)
        .host_status(this.host_status)
        .host_monitoring_agent_task_status(this.host_monitoring_agent_task_status)
        .host_log_agent_task_status(this.host_log_agent_task_status)
        .host_monitoring_agent_task_id(this.host_monitoring_agent_task_id)
        .host_log_agent_task_id(this.host_log_agent_task_id)
        .monitoringServiceStatus(this.monitoringServiceStatus)
        .logServiceStatus(this.logServiceStatus)
        .monitoring_agent_config_git_hash(this.monitoring_agent_config_git_hash)
        .log_agent_config_git_hash(this.log_agent_config_git_hash)
        .monitoring_agent_version(this.monitoring_agent_version)
        .log_agent_version(this.log_agent_version)
        .createdAt(this.createdAt)
        .updatedAt(this.updatedAt)
        .build();
  }
}