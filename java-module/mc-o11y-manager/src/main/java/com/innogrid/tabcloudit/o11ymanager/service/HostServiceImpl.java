package com.innogrid.tabcloudit.o11ymanager.service;


import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostCreateDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostUpdateDTO;
import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.enums.AgentServiceStatus;
import com.innogrid.tabcloudit.o11ymanager.event.HostUpdateNotifyMultipleEvent;
import com.innogrid.tabcloudit.o11ymanager.event.HostUpdateNotifySingleEvent;
import com.innogrid.tabcloudit.o11ymanager.exception.agent.LogAgentNotInstalled;
import com.innogrid.tabcloudit.o11ymanager.exception.agent.MonitoringAgentNotInstalled;
import com.innogrid.tabcloudit.o11ymanager.exception.host.DuplicatedHostIpException;
import com.innogrid.tabcloudit.o11ymanager.exception.host.HostAgentTaskProcessingException;
import com.innogrid.tabcloudit.o11ymanager.exception.host.HostNotExistException;
import com.innogrid.tabcloudit.o11ymanager.global.aspect.request.RequestInfo;
import com.innogrid.tabcloudit.o11ymanager.global.error.ResourceNotExistsException;
import com.innogrid.tabcloudit.o11ymanager.infrastructure.util.ChaCha20Poly3105Util;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.repository.HostJpaRepository;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.HostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HostServiceImpl implements HostService {

  private final HostJpaRepository hostJpaRepository;
  private final RequestInfo requestInfo;
  private final ApplicationEventPublisher event;

  @Override
  public List<HostDTO> list() {
    return hostJpaRepository.findAll().stream().map(HostDTO::fromEntity).toList();
  }

  @Override
  public HostDTO findById(String id) {
    HostEntity host = hostJpaRepository.findById(id).orElse(null);
    if (host == null) {
      throw new HostNotExistException(requestInfo.getRequestId(), id);
    }
    return HostDTO.fromEntity(host);
  }

  @Override
  public HostDTO create(HostCreateDTO dto, String ip) {

    // 비밀번호 암호화
    String encryptedPassword = null;

    try {
      if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
        encryptedPassword = ChaCha20Poly3105Util.encryptString(dto.getPassword());
      }
    } catch (Exception e) {
      throw new RuntimeException("Password encryption failed", e);
    }

    HostEntity entity = HostEntity.builder().
        id(dto.getId()).
        name(dto.getName()).
        ip(ip).
        port(dto.getPort()).
        user(dto.getUser()).
        password(encryptedPassword).
        description(dto.getDescription()).
        credential_id(dto.getCredentialId()).
        cloud_service(dto.getCloudService()).
        type(dto.getType()).
        host_monitoring_agent_task_status(HostAgentTaskStatus.IDLE).
        host_log_agent_task_status(HostAgentTaskStatus.IDLE).
        build();

    HostEntity result = hostJpaRepository.save(entity);

    event.publishEvent(
        HostUpdateNotifyMultipleEvent.builder()
            .build()
    );

    return HostDTO.fromEntity(result);
  }

  @Override
  public HostDTO update(String id, HostUpdateDTO dto) {
    HostEntity host = hostJpaRepository.findById(id)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", id));

    host.setUser(dto.getData().getUser());
    host.setPassword(dto.getData().getPassword());
    host.setPort(dto.getData().getPort());

    HostEntity result = hostJpaRepository.save(host);

    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(result.getId())
            .build()
    );
    event.publishEvent(
        HostUpdateNotifyMultipleEvent.builder()
            .build()
    );

    return HostDTO.fromEntity(result);
  }

  @Override
  public void deleteById(String id) {
    hostJpaRepository.deleteById(id);

    event.publishEvent(
        HostUpdateNotifyMultipleEvent.builder()
            .build()
    );

  }

  @Override
  public boolean existsById(String id) {
    return hostJpaRepository.existsById(id);
  }

  @Override
  public void validateUniqueIpPort(String ip, int port) throws DuplicatedHostIpException {
    List<HostEntity> found = hostJpaRepository.findByIpAndPort(ip, port);
    if (!found.isEmpty()) {
      throw new DuplicatedHostIpException(requestInfo.getRequestId(),
          "중복된 IP:Port (" + ip + ":" + port + ")");
    }
  }

  @Override
  public void isIdleMonitoringAgent(String hostId) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    if (host.getHost_monitoring_agent_task_status() != HostAgentTaskStatus.IDLE) {
      throw new HostAgentTaskProcessingException(requestInfo.getRequestId(), hostId, "모니터링",
          host.getHost_monitoring_agent_task_status());
    }
  }

  public void isIdleLogAgent(String hostId) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    if (host.getHost_log_agent_task_status() != HostAgentTaskStatus.IDLE) {
      throw new HostAgentTaskProcessingException(requestInfo.getRequestId(), hostId, "로그",
          host.getHost_log_agent_task_status());
    }

  }

  @Override
  public void isLogAgentInstalled(String hostId) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    if (host.getLogServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
      throw new LogAgentNotInstalled(requestInfo.getRequestId(), hostId);
    }
  }

  @Override
  public void isMonitoringAgentInstalled(String hostId) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    if (host.getLogServiceStatus().equals(AgentServiceStatus.NOT_EXIST.toString())) {
      throw new MonitoringAgentNotInstalled(requestInfo.getRequestId(), hostId);
    }
  }

  @Override
  public void updateMonitoringAgentTaskStatus(String hostId, HostAgentTaskStatus status) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));


    host.setHost_monitoring_agent_task_status(status);

    // TODO 다른 방법 고민할 것
    if (status.equals(HostAgentTaskStatus.IDLE)) {
      host.setHost_monitoring_agent_task_id("");
    }

    hostJpaRepository.save(host);
    log.info("[HostService] Monitoring Service Status {}: {}",
        host.getHost_monitoring_agent_task_id(), host.getHost_monitoring_agent_task_status());
    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(hostId)
            .build()
    );
  }

  @Override
  public void updateLogAgentTaskStatus(String hostId, HostAgentTaskStatus status) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    host.setHost_log_agent_task_status(status);

    // TODO 다른 방법 고민할 것
    if (status.equals(HostAgentTaskStatus.IDLE)) {
      host.setHost_log_agent_task_id("");
    }

    hostJpaRepository.save(host);
    log.info("[HostService] Log Service Status {}: {}", host.getHost_log_agent_task_id(),
        host.getHost_log_agent_task_status());
    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(hostId)
            .build()
    );
  }

  @Override
  public void updateMonitoringAgentConfigGitHash(String hostId, String commitHash) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    host.setMonitoring_agent_config_git_hash(commitHash);

    hostJpaRepository.save(host);
    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(hostId)
            .build()
    );
  }

  @Override
  public void updateLogAgentConfigGitHash(String hostId, String commitHash) {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    host.setLog_agent_config_git_hash(commitHash);

    hostJpaRepository.save(host);
    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(hostId)
            .build()
    );
  }

  @Override
  public HostConnectionDTO getHostConnectionInfo(String hostId) throws Exception {
    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    return HostConnectionDTO.builder()
        .hostId(host.getId())
        .ip(host.getIp())
        .userId(host.getUser())
        .password(ChaCha20Poly3105Util.decryptString(host.getPassword()))
        .port(host.getPort())
        .build();
  }

  @Override
  public void updateHostAgentTaskStatusAndTaskId(String hostId, HostAgentTaskStatus status,
      String taskId) {

    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    host.setHost_monitoring_agent_task_status(status);
    host.setHost_monitoring_agent_task_id(taskId);

    hostJpaRepository.save(host);
    log.info("[HostService] Monitoring Service Status {}", host);
    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(hostId)
            .build()
    );

  }

  @Override
  public void updateLogAgentTaskStatusAndTaskId(String hostId, HostAgentTaskStatus status,
      String taskId) {

    HostEntity host = hostJpaRepository.findById(hostId)
        .orElseThrow(
            () -> new ResourceNotExistsException(requestInfo.getRequestId(), "HostEntity", hostId));

    host.setHost_log_agent_task_status(status);
    host.setHost_log_agent_task_id(taskId);

    hostJpaRepository.save(host);
    event.publishEvent(
        HostUpdateNotifySingleEvent.builder()
            .hostId(hostId)
            .build()
    );

    log.info("[HostService] Log Service Status {}", host);
  }
}
