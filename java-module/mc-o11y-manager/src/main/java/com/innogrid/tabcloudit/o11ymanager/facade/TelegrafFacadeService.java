package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ResultDTO;
import com.innogrid.tabcloudit.o11ymanager.entity.HostEntity;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.enums.AgentAction;
import com.innogrid.tabcloudit.o11ymanager.enums.ResponseStatus;
import com.innogrid.tabcloudit.o11ymanager.enums.SemaphoreInstallMethod;
import com.innogrid.tabcloudit.o11ymanager.event.AgentHistoryEvent;
import com.innogrid.tabcloudit.o11ymanager.event.AgentHistoryFailEvent;
import com.innogrid.tabcloudit.o11ymanager.exception.config.FileReadingException;
import com.innogrid.tabcloudit.o11ymanager.global.aspect.request.RequestInfo;
import com.innogrid.tabcloudit.o11ymanager.infrastructure.util.ChaCha20Poly3105Util;
import com.innogrid.tabcloudit.o11ymanager.infrastructure.util.CheckUtil;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Task;
import com.innogrid.tabcloudit.o11ymanager.service.domain.SemaphoreDomainService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.HostService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.SshService;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegrafFacadeService {

  private final HostService hostService;
  private static final Lock agentTaskStatusLock = new ReentrantLock();
  private final RequestInfo requestInfo;
  private final ApplicationEventPublisher event;
  private final SemaphoreDomainService semaphoreDomainService;
  private final FileFacadeService fileFacadeService;
  private final SshService sshService;
  private final SchedulerFacadeService schedulerFacadeService;
  private final TelegrafConfigFacadeService telegrafConfigFacadeService;

  public void install(@NotBlank String hostId, @NotBlank String requestUserId,
      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    hostService.isIdleMonitoringAgent(hostId);

    // 2. host 상태 업데이트
    hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.INSTALLING);

    // 3. 로컬 파일 확인
    HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);

    String configContent;
    try {
      configContent = fileFacadeService.readAgentConfigFile(hostConnectionInfo.getHostId(),
          Agent.TELEGRAF);
    } catch (FileReadingException e) {
      // 로컬에 파일이 없을 경우 생성
      HostDTO hostDTO = hostService.findById(hostId);
      telegrafConfigFacadeService.initTelegrafConfig(hostConnectionInfo, hostDTO.getType(),
          hostDTO.getCredentialId(), hostDTO.getCloudService());

      // 다시 읽기
      configContent = fileFacadeService.readAgentConfigFile(hostConnectionInfo.getHostId(),
          Agent.TELEGRAF);
    }

    // 4. 전송(semaphore) - 설치 요청
    Task task = semaphoreDomainService.install(hostConnectionInfo, SemaphoreInstallMethod.INSTALL,
        configContent, Agent.TELEGRAF,
        templateCount);

    // 5. task ID, task status 업데이트
    hostService.updateMonitoringAgentTaskStatusAndTaskId(hostId, HostAgentTaskStatus.INSTALLING,
        String.valueOf(task.getId()));

    // 6. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .requestUserId(requestUserId)
        .hostId(hostId)
        .agentAction(AgentAction.MONITORING_AGENT_INSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);

    // 7. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), hostId,
        SemaphoreInstallMethod.INSTALL, Agent.TELEGRAF, requestUserId);
  }

  public void update(@NotBlank String hostId, @NotBlank String requestUserId,
                      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    hostService.isIdleMonitoringAgent(hostId);

    // 2. host 상태 업데이트
    hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.UPDATING);

    // 3. 전송(semaphore) - 업데이트 요청
    HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);
    Task task = semaphoreDomainService.install(hostConnectionInfo, SemaphoreInstallMethod.UPDATE,
            null, Agent.TELEGRAF,
            templateCount);

    // 4. task ID, task status 업데이트
    hostService.updateMonitoringAgentTaskStatusAndTaskId(hostId, HostAgentTaskStatus.UPDATING,
            String.valueOf(task.getId()));

    // 5. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .hostId(hostId)
            .agentAction(AgentAction.MONITORING_AGENT_UPDATE_STARTED)
            .reason("")
            .build();

    event.publishEvent(successEvent);

    // 6. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), hostId,
            SemaphoreInstallMethod.UPDATE, Agent.TELEGRAF, requestUserId);
  }

  public void uninstall(String hostId, int templateCount, String requestUserId) throws Exception {

    // 1) 상태 확인
    hostService.isIdleMonitoringAgent(hostId);

    // 2) 상태 변경
    hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);

    // 3) 전송(semaphore) - 삭제 요청
    HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);

    Task task = semaphoreDomainService.install(hostConnectionInfo, SemaphoreInstallMethod.UNINSTALL,
        null, Agent.TELEGRAF,
        templateCount);

    // 4) task ID, task status 업데이트
    hostService.updateMonitoringAgentTaskStatusAndTaskId(hostId, HostAgentTaskStatus.UNINSTALLING,
        String.valueOf(task.getId()));

    // 5) 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .requestUserId(requestUserId)
        .hostId(hostId)
        .agentAction(AgentAction.MONITORING_AGENT_UNINSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);


    // 6) 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), hostId,
        SemaphoreInstallMethod.UNINSTALL, Agent.TELEGRAF, requestUserId);
  }


  @Transactional
  public List<ResultDTO> enable(String[] ids, String requestUserId) {
    List<ResultDTO> results = new ArrayList<>();

    for (String id : CheckUtil.emptyIfNull(List.of(ids))) {

      try {
        agentTaskStatusLock.lock();

        HostEntity host;
        host = hostService.findById(id).toEntity();

        hostService.isIdleMonitoringAgent(id);

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.ENABLING);

        host.setPassword(ChaCha20Poly3105Util.decryptString(host.getPassword()));

        SshConnection connection = sshService.getConnection(
            host.getIp(), host.getPort(), host.getUser(), host.getPassword());

        sshService.enableTelegraf(connection, host.getIp(), host.getPort(),
            host.getUser(), host.getPassword());

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .reason("")
            .agentAction(AgentAction.ENABLE_TELEGRAF)
            .hostId(id)
            .build();

        event.publishEvent(successEvent);

        results.add(ResultDTO.builder()
            .id(host.getId())
            .status(ResponseStatus.SUCCESS)
            .build());

        agentTaskStatusLock.unlock();
      } catch (Exception e) {

        agentTaskStatusLock.unlock();

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .reason(e.getMessage())
            .agentAction(AgentAction.ENABLE_TELEGRAF)
            .hostId(id)
            .build();

        event.publishEvent(failEvent);

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      }
    }
    return results;
  }

  @Transactional
  public List<ResultDTO> disable(String[] ids, String requestUserId) {

    List<ResultDTO> results = new ArrayList<>();

    for (String id : CheckUtil.emptyIfNull(List.of(ids))) {

      try {
        agentTaskStatusLock.lock();

        HostEntity host;
        host = hostService.findById(id).toEntity();

        hostService.isIdleMonitoringAgent(id);

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.DISABLING);

        host.setPassword(ChaCha20Poly3105Util.decryptString(host.getPassword()));

        SshConnection connection = sshService.getConnection(
            host.getIp(), host.getPort(), host.getUser(), host.getPassword());

        sshService.disableTelgraf(connection, host.getIp(), host.getPort(),
            host.getUser(), host.getPassword());

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .hostId(id)
            .agentAction(AgentAction.DISABLE_TELEGRAF)
            .reason("")
            .build();
        event.publishEvent(successEvent);

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());

        agentTaskStatusLock.unlock();
      } catch (Exception e) {

        agentTaskStatusLock.unlock();

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.DISABLE_TELEGRAF)
            .reason(e.getMessage())
            .hostId(id)
            .build();
        event.publishEvent(failEvent);

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      }
    }
    return results;
  }

  @Transactional
  public List<ResultDTO> restart(String[] ids, String requestUserId) {

    List<ResultDTO> results = new ArrayList<>();

    for (String id : CheckUtil.emptyIfNull(List.of(ids))) {

      try {
        agentTaskStatusLock.lock();

        HostEntity host;

        host = hostService.findById(id).toEntity();

        hostService.isIdleMonitoringAgent(id);

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.RESTARTING);

        host.setPassword(ChaCha20Poly3105Util.decryptString(host.getPassword()));

        SshConnection connection = sshService.getConnection(
            host.getIp(), host.getPort(), host.getUser(), host.getPassword());

        sshService.restartTelegraf(connection, host.getIp(), host.getPort(),
            host.getUser(), host.getPassword());

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.RESTART_TELEGRAF)
            .hostId(id)
            .reason("")
            .build();

        event.publishEvent(successEvent);

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());
        agentTaskStatusLock.unlock();
      } catch (Exception e) {
        agentTaskStatusLock.unlock();

        hostService.updateMonitoringAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.RESTART_TELEGRAF)
            .reason(e.getMessage())
            .hostId(id)
            .build();

        event.publishEvent(failEvent);

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      }
    }
    return results;
  }


}
