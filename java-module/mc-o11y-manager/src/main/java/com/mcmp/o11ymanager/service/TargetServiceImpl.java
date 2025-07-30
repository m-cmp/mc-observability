package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.entity.TargetId;
import com.mcmp.o11ymanager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.exception.agent.LogAgentNotInstalled;
import com.mcmp.o11ymanager.exception.agent.MonitoringAgentNotInstalled;
import com.mcmp.o11ymanager.exception.target.TargetAgentTaskProcessingException;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetStatus;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TargetServiceImpl implements TargetService {

  private final TargetJpaRepository targetJpaRepository;
  private final RequestInfo requestInfo;
  private final ApplicationEventPublisher event;

  @Override
  public TargetDTO get(String nsId, String mciId, String targetId) {
    TargetEntity entity = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", nsId + "/" + mciId));
    return com.mcmp.o11ymanager.dto.target.TargetDTO.fromEntity(entity);
  }

  @Override
  public List<TargetDTO> getByNsMci(String nsId, String mciId) {
    return targetJpaRepository.findByNsIdAndMciId(nsId, mciId)
            .stream().map(com.mcmp.o11ymanager.dto.target.TargetDTO::fromEntity).toList();
  }

  @Override
  public List<TargetDTO> list() {
    return targetJpaRepository.findAll().stream().map(com.mcmp.o11ymanager.dto.target.TargetDTO::fromEntity).toList();
  }

  @Override
  public TargetDTO post(String nsId, String mciId, String targetId, TargetStatus targetStatus, TargetRequestDTO dto) {
    TargetEntity target = TargetEntity.builder()
        .nsId(nsId)
        .mciId(mciId)
        .targetId(targetId)
        .name(dto.getName())
        .description(dto.getDescription())
        .nsId(nsId)
        .mciId(mciId)
        .targetStatus(targetStatus)
        .build();

    TargetEntity savedTarget = targetJpaRepository.save(target);

    return TargetDTO.fromEntity(savedTarget);
  }



  @Override
  public TargetDTO put(String nsId, String mciId, String targetId, TargetRequestDTO dto) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", targetId));


    if (dto.getName() != null)        target.setName(dto.getName());
    if (dto.getDescription() != null) target.setDescription(dto.getDescription());


    TargetEntity updated = targetJpaRepository.save(target);

    return TargetDTO.fromEntity(updated);
  }



  @Override
  @Transactional
  public void delete(String nsId, String mciId, String targetId) {
    TargetEntity entity = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", targetId));

    targetJpaRepository.delete(entity);
  }

  @Override
  public void isIdleMonitoringAgent(String nsId, String mciId, String targetId) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

    if (target.getMonitoringAgentTaskStatus() != TargetAgentTaskStatus.IDLE) {
      throw new TargetAgentTaskProcessingException(requestInfo.getRequestId(), targetId, "monitoringAgentTask",
          target.getMonitoringAgentTaskStatus());
    }
  }

  @Override
  public void isIdleLogAgent(String nsId, String mciId, String targetId) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

    if (target.getLogAgentTaskStatus() != TargetAgentTaskStatus.IDLE) {
      throw new TargetAgentTaskProcessingException(requestInfo.getRequestId(), targetId, "로그",
          target.getLogAgentTaskStatus());
    }
  }

  @Override
  public void isLogAgentInstalled(String nsId, String mciId, String targetId) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

    if (target.getLogServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
      throw new LogAgentNotInstalled(requestInfo.getRequestId(), targetId);
    }
  }

  @Override
  public void isMonitoringAgentInstalled(String nsId, String mciId, String targetId) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

    if (target.getMonitoringServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
      throw new MonitoringAgentNotInstalled(requestInfo.getRequestId(), targetId);
    }
  }

  @Override
  public void updateMonitoringAgentTaskStatus(String nsId, String mciId, String targetId, TargetAgentTaskStatus status) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

    target.setMonitoringAgentTaskStatus(status);

    // TODO 다른 방법 고민할 것
    if (status.equals(TargetAgentTaskStatus.IDLE)) {
      target.setTargetMonitoringAgentTaskId("");
    }

    targetJpaRepository.save(target);
    log.info("[TargetService] Monitoring Service Status {}: {}",
        target.getTargetMonitoringAgentTaskId(), target.getMonitoringAgentTaskStatus());
  }

  @Override
  public void updateLogAgentTaskStatus(String nsId, String mciId, String targetId, TargetAgentTaskStatus status) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

    target.setLogAgentTaskStatus(status);

    // TODO 다른 방법 고민할 것
    if (status.equals(TargetAgentTaskStatus.IDLE)) {
      target.setTargetLogAgentTaskId("");
    }

    targetJpaRepository.save(target);
    log.info("[TargetService] Log Service Status {}: {}", target.getTargetLogAgentTaskId(),
        target.getLogServiceStatus());
  }



  @Override
  public void updateMonitoringAgentTaskStatusAndTaskId(String nsId, String mciId, String targetId, TargetAgentTaskStatus status,
      String taskId) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

    target.setMonitoringAgentTaskStatus(status);
    target.setTargetMonitoringAgentTaskId(taskId);

    targetJpaRepository.save(target);
    log.info("==================================Monitoring Service Status {}===============================================", target);
  }

  @Override
  public void updateLogAgentTaskStatusAndTaskId(String nsId, String mciId, String targetId, TargetAgentTaskStatus status,
      String taskId) {
    TargetEntity target = targetJpaRepository.findByNsIdAndMciIdAndTargetId(nsId, mciId, targetId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity",
                targetId));

    target.setLogAgentTaskStatus(status);
    target.setTargetLogAgentTaskId(taskId);

    targetJpaRepository.save(target);

    log.info("[TargetService] Log Service Status {}", target);
  }
}





