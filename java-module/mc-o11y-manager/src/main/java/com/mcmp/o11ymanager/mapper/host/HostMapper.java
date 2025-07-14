package com.mcmp.o11ymanager.mapper.host;

import com.mcmp.o11ymanager.dto.host.HostCreateDTO;
import com.mcmp.o11ymanager.dto.host.HostResponseDTO;
import com.mcmp.o11ymanager.dto.host.HostUpdateDTO;
import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.global.definition.TimestampDefinition;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class HostMapper {

  public HostResponseDTO toDTO(HostEntity host) {
    return HostResponseDTO.builder()
        .id(host.getId())
        .name(host.getName())
        .credentialId(host.getCredential_id())
        .cloudService(host.getCloud_service())
        .hostname(host.getHostname())
        .ip(host.getIp())
        .port(host.getPort())
        .hostStatus(host.getHost_status())
        .monitoringServiceStatus(host.getMonitoringServiceStatus())
        .logServiceStatus(host.getLogServiceStatus())
        .monitoringAgentConfigGitHash(host.getMonitoring_agent_config_git_hash())
        .logAgentConfigGitHash(host.getLog_agent_config_git_hash())
        .monitoringAgentVersion(host.getMonitoring_agent_version())
        .logAgentVersion(host.getLog_agent_version())
        .hostMonitoringAgentTaskStatus(host.getHost_monitoring_agent_task_status())
        .hostLogAgentTaskStatus(host.getHost_log_agent_task_status())
        .description(host.getDescription())
        .type(host.getType())
        .user(host.getUser())
        .createdAt(host.getCreatedAt() != null ? host.getCreatedAt()
            .format(DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
        .updatedAt(host.getUpdatedAt() != null ? host.getUpdatedAt()
            .format(DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
        .build();
  }

  public HostEntity fromResponseDTO(HostResponseDTO dto) {
    return HostEntity.builder()
        .id(dto.getId())
        .name(dto.getName())
        .hostname(dto.getHostname())
        .credential_id(dto.getCredentialId())
        .cloud_service(dto.getCloudService())
        .ip(dto.getIp())
        .port(dto.getPort())
        .host_status(dto.getHostStatus())
        .monitoringServiceStatus(dto.getMonitoringServiceStatus())
        .logServiceStatus(dto.getLogServiceStatus())
        .monitoring_agent_config_git_hash(dto.getMonitoringAgentConfigGitHash())
        .log_agent_config_git_hash(dto.getLogAgentConfigGitHash())
        .monitoring_agent_version(dto.getMonitoringAgentVersion())
        .log_agent_version(dto.getLogAgentVersion())
        .description(dto.getDescription())
        .type(dto.getType())
        .user(dto.getUser())
        .createdAt(dto.getCreatedAt() != null ? LocalDateTime.parse(dto.getCreatedAt(),
            DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
        .updatedAt(dto.getUpdatedAt() != null ? LocalDateTime.parse(dto.getUpdatedAt(),
            DateTimeFormatter.ofPattern(TimestampDefinition.TIMESTAMP_FORMAT)) : null)
        .build();
  }

  public HostEntity fromCreateDTO(HostCreateDTO dto, String ip) {
    return HostEntity.builder()
        .id(dto.getId())
        .name(dto.getName())
        .ip(ip)
        .credential_id(dto.getCredentialId())
        .cloud_service(dto.getCloudService())
        .port(dto.getPort())
        .user(dto.getUser())
        .password(dto.getPassword())
        .description(dto.getDescription())
        .type(dto.getType())
        .build();
  }


  public void fromUpdateDTO(HostUpdateDTO dto, HostEntity host) {
    if (dto.getData().getPort() != null && dto.getData().getPort() > 0) {
      host.setPort(dto.getData().getPort());
    }

    if (dto.getData().getUser() != null && !dto.getData().getUser().isBlank()) {
      host.setUser(dto.getData().getUser());
    }

    if (dto.getData().getPassword() != null && !dto.getData().getPassword().isBlank()) {
      host.setPassword(dto.getData().getPassword());
    }
  }
}
