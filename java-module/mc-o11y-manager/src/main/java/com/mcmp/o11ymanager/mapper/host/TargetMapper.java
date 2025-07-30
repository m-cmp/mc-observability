package com.mcmp.o11ymanager.mapper.host;

import com.mcmp.o11ymanager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
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
        .targetStatus(dto.getTargetStatus())
        .mciId(dto.getMciId())
        .name(dto.getName())
        .description(dto.getDescription())
        .monitoringAgentTaskStatus(dto.getMonitoringAgentTaskStatus())
        .logAgentTaskStatus(dto.getLogAgentTaskStatus())
        .targetMonitoringAgentTaskId(dto.getTargetMonitoringAgentTaskId())
        .targetLogAgentTaskId(dto.getTargetLogAgentTaskId())
        .monitoringServiceStatus(dto.getMonitoringServiceStatus())
        .logServiceStatus(dto.getLogServiceStatus())
        .nsId(dto.getNsId())
        .mciId(dto.getMciId())
        .build();
  }

  public TargetEntity fromCreateDTO(TargetRequestDTO dto) {
    return TargetEntity.builder()
        .name(dto.getName())
        .description(dto.getDescription())
        .build();
  }

  public void fromUpdateDTO(TargetRequestDTO dto, TargetEntity target) {
    if (dto.getName() != null && !dto.getName().isBlank()) {
      target.setName(dto.getName());
    }


    if (dto.getDescription() != null) {
      target.setDescription(dto.getDescription());
    }
  }
}