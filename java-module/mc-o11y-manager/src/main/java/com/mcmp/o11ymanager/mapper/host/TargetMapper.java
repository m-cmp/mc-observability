package com.mcmp.o11ymanager.mapper.host;

import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.entity.TargetEntity;
import org.springframework.stereotype.Component;

@Component
public class TargetMapper {

  public TargetDTO toDTO(TargetEntity target) {
    return TargetDTO.fromEntity(target);
  }

  public TargetEntity fromResponseDTO(TargetDTO dto) {
    return TargetEntity.builder()
        .targetId(dto.getTargetId())
        .nsId(dto.getNsId())
        .mciId(dto.getMciId())
        .name(dto.getName())
        .aliasName(dto.getAliasName())
        .description(dto.getDescription())
        .targetStatus(dto.getTargetStatus())
        .monitoringAgentTaskStatus(dto.getMonitoringAgentTaskStatus())
        .logAgentTaskStatus(dto.getLogAgentTaskStatus())
        .targetMonitoringAgentTaskId(dto.getTargetMonitoringAgentTaskId())
        .targetLogAgentTaskId(dto.getTargetLogAgentTaskId())
        .monitoringServiceStatus(dto.getMonitoringServiceStatus())
        .logServiceStatus(dto.getLogServiceStatus())
        .createdAt(dto.getCreatedAt())
        .updatedAt(dto.getUpdatedAt())
        .nsId(dto.getNsId())
        .mciId(dto.getMciId())
        .state(dto.getState())
        .build();
  }

  public TargetEntity fromCreateDTO(TargetRegisterDTO dto) {
    return TargetEntity.builder()
        .name(dto.getName())
        .aliasName(dto.getAliasName())
        .description(dto.getDescription())
        .build();
  }

  public void fromUpdateDTO(TargetUpdateDTO dto, TargetEntity target) {
    if (dto.getName() != null && !dto.getName().isBlank()) {
      target.setName(dto.getName());
    }

    if (dto.getAliasName() != null) {
      target.setAliasName(dto.getAliasName());
    }

    if (dto.getDescription() != null) {
      target.setDescription(dto.getDescription());
    }
  }
}