package com.mcmp.o11ymanager.service;

import com.mcmp.o11ymanager.dto.host.HostConnectionDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.entity.AccessInfoEntity;
import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.event.HostUpdateNotifyMultipleEvent;
import com.mcmp.o11ymanager.event.HostUpdateNotifySingleEvent;
import com.mcmp.o11ymanager.exception.agent.LogAgentNotInstalled;
import com.mcmp.o11ymanager.exception.agent.MonitoringAgentNotInstalled;
import com.mcmp.o11ymanager.exception.host.HostAgentTaskProcessingException;
import com.mcmp.o11ymanager.exception.target.TargetAgentTaskProcessingException;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.global.error.ResourceNotExistsException;
import com.mcmp.o11ymanager.infrastructure.util.ChaCha20Poly3105Util;
import com.mcmp.o11ymanager.model.host.HostAgentTaskStatus;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.repository.TargetJpaRepository;
import com.mcmp.o11ymanager.oldService.domain.interfaces.TargetService;
import java.util.Optional;
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
  public Optional<TargetEntity> findById(String targetId) {
    return targetJpaRepository.findById(targetId);
  }

  @Override
  public TargetDTO get(String nsId, String mciId, String targetId) {
    TargetEntity entity = targetJpaRepository.findByNsIdAndMciIdTargetId(nsId, mciId, targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", nsId + "/" + mciId));
    return com.mcmp.o11ymanager.dto.target.TargetDTO.fromEntity(entity);
  }


  @Override
  public TargetDTO getByNsMci(String nsId, String mciId) {
    TargetEntity entity = targetJpaRepository.findByNsIdAndMciId(nsId, mciId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", nsId + "/" + mciId));
    return com.mcmp.o11ymanager.dto.target.TargetDTO.fromEntity(entity);
  }


  @Override
  public List<TargetDTO> list() {
    return targetJpaRepository.findAll().stream().map(com.mcmp.o11ymanager.dto.target.TargetDTO::fromEntity).toList();
  }

  @Override
  public TargetDTO post(String nsId, String mciId, String targetId, TargetRegisterDTO dto) {

    String encryptedSshKey = null;

    try {
      if (dto.getAccessInfo() != null &&
          dto.getAccessInfo().getSshKey() != null &&
          !dto.getAccessInfo().getSshKey().isBlank()) {

        encryptedSshKey = ChaCha20Poly3105Util.encryptString(dto.getAccessInfo().getSshKey());
      }
    } catch (Exception e) {
      throw new RuntimeException("SSH Key encryption failed", e);
    }

    TargetEntity target = TargetEntity.builder()
        .id(targetId)
        .name(dto.getName())
        .aliasName(dto.getAliasName())
        .description(dto.getDescription())
        .csp(dto.getCsp())
        .nsId(nsId)
        .mciId(mciId)
        .subGroup(dto.getSubGroup())
        .build();

    AccessInfoEntity accessInfoEntity = AccessInfoEntity.builder()
        .id(targetId)
        .ip(dto.getAccessInfo().getIp())
        .port(dto.getAccessInfo().getPort())
        .user(dto.getAccessInfo().getUser())
        .sshKey(encryptedSshKey)
        .target(target)
        .build();

    target.setAccessInfo(accessInfoEntity);

    TargetEntity saved = targetJpaRepository.save(target);

    event.publishEvent(
        HostUpdateNotifyMultipleEvent.builder().build()
    );

    return TargetDTO.fromEntity(saved);
  }


  @Override
  public TargetDTO put(String targetId, String nsId, String mciId, TargetUpdateDTO request) {
    TargetEntity target = targetJpaRepository.findById(targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", targetId));

    if (!nsId.equals(target.getNsId()) || !mciId.equals(target.getMciId())) {
      throw new IllegalArgumentException("failed to update target");
    }

    if (request.getName() != null) {
      target.setName(request.getName());
    }

    if (request.getAliasName() != null) {
      target.setAliasName(request.getAliasName());
    }

    if (request.getDescription() != null) {
      target.setDescription(request.getDescription());
    }

    TargetEntity updated = targetJpaRepository.save(target);

    return com.mcmp.o11ymanager.dto.target.TargetDTO.fromEntity(updated);
  }


  @Override
  @Transactional
  public void delete(String targetId, String nsId, String mciId) {
    TargetEntity entity = targetJpaRepository.findById(targetId)
        .orElseThrow(() -> new ResourceNotExistsException(
            requestInfo.getRequestId(), "TargetEntity", targetId));

    if (!nsId.equals(entity.getNsId()) || !mciId.equals(entity.getMciId())) {
      throw new IllegalArgumentException("nsId 또는 mciId가 일치하지 않습니다.");
    }

    targetJpaRepository.deleteById(targetId);
  }


@Override
public void isIdleMonitoringAgent(String targetId) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  if (target.getMonitoringTaskStatus() != TargetAgentTaskStatus.IDLE) {
    throw new TargetAgentTaskProcessingException(requestInfo.getRequestId(), targetId, "모니터링",
        target.getMonitoringTaskStatus());
  }
}

public void isIdleLogAgent(String targetId) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  if (target.getLogTaskStatus() != TargetAgentTaskStatus.IDLE) {
    throw new TargetAgentTaskProcessingException(requestInfo.getRequestId(), targetId, "로그",
        target.getLogTaskStatus());
  }
}

@Override
public void isLogAgentInstalled(String targetId) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  if (target.getLogServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
    throw new LogAgentNotInstalled(requestInfo.getRequestId(), targetId);
  }
}

@Override
public void isMonitoringAgentInstalled(String targetId) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  if (target.getMonitoringServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
    throw new MonitoringAgentNotInstalled(requestInfo.getRequestId(), targetId);
  }
}

@Override
public void updateMonitoringAgentTaskStatus(String targetId, TargetAgentTaskStatus status) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  target.setMonitoringTaskStatus(status);

  // TODO 다른 방법 고민할 것
  if (status.equals(TargetAgentTaskStatus.IDLE)) {
    target.setTargetMonitoringAgentTaskId("");
  }

  targetJpaRepository.save(target);
  log.info("[TargetService] Monitoring Service Status {}: {}",
      target.getTargetMonitoringAgentTaskId(), target.getMonitoringTaskStatus());
  event.publishEvent(
      HostUpdateNotifySingleEvent.builder()
          .hostId(targetId)
          .build()
  );
}

@Override
public void updateLogAgentTaskStatus(String targetId, TargetAgentTaskStatus status) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  target.setLogTaskStatus(status);

  // TODO 다른 방법 고민할 것
  if (status.equals(TargetAgentTaskStatus.IDLE)) {
    target.setTargetLogAgentTaskId("");
  }

  targetJpaRepository.save(target);
  log.info("[TargetService] Log Service Status {}: {}", target.getTargetLogAgentTaskId(),
      target.getLogServiceStatus());
  event.publishEvent(
      HostUpdateNotifySingleEvent.builder()
          .hostId(targetId)
          .build()
  );
}

@Override
public void updateMonitoringAgentConfigGitHash(String targetId, String commitHash) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  targetJpaRepository.save(target);
  event.publishEvent(
      HostUpdateNotifySingleEvent.builder()
          .hostId(targetId)
          .build()
  );
}

@Override
public void updateLogAgentConfigGitHash(String targetId, String commitHash) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));


  targetJpaRepository.save(target);
  event.publishEvent(
      HostUpdateNotifySingleEvent.builder()
          .hostId(targetId)
          .build()
  );
}

@Override
public TargetRegisterDTO getTargetInfo(String targetId) throws Exception {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  return TargetRegisterDTO.builder()
      .name(target.getId())
      .aliasName(target.getAliasName())
      .description(target.getDescription())
      .csp(target.getCsp())
      .subGroup(target.getSubGroup())
      .state(target.getState())
      .build();
}

@Override
public TargetRegisterDTO.AccessInfoDTO getAccessInfo(String targetId) throws Exception {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));


  return TargetRegisterDTO.AccessInfoDTO.builder()
      .ip(target.getAccessInfo().getIp())
      .user(target.getAccessInfo().getUser())
      .port(target.getAccessInfo().getPort())
      .sshKey(ChaCha20Poly3105Util.decryptString(target.getAccessInfo().getSshKey()))
      .build();
}

@Override
public void updateMonitoringAgentTaskStatusAndTaskId(String targetId, TargetAgentTaskStatus status,
    String taskId) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity", targetId));

  target.setMonitoringTaskStatus(status);
  target.setTargetMonitoringAgentTaskId(taskId);

  targetJpaRepository.save(target);
  log.info("[TargetService] Monitoring Service Status {}", target);
  event.publishEvent(
      HostUpdateNotifySingleEvent.builder()
          .hostId(targetId)
          .build()
  );
}

@Override
public void updateLogAgentTaskStatusAndTaskId(String targetId, TargetAgentTaskStatus status,
    String taskId) {
  TargetEntity target = targetJpaRepository.findById(targetId)
      .orElseThrow(
          () -> new ResourceNotExistsException(requestInfo.getRequestId(), "TargetEntity",
              targetId));

  target.setLogTaskStatus(status);
  target.setTargetLogAgentTaskId(taskId);

  targetJpaRepository.save(target);
  event.publishEvent(
      HostUpdateNotifySingleEvent.builder()
          .hostId(targetId)
          .build()
  );

  log.info("[TargetService] Log Service Status {}", target);
}


  @Override
  public boolean existsById(String targetId) {
    return targetJpaRepository.existsById(targetId);
  }


}





