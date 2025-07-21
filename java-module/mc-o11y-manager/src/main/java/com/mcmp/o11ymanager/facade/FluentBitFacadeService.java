package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.host.HostConnectionDTO;
import com.mcmp.o11ymanager.dto.host.HostDTO;
import com.mcmp.o11ymanager.dto.host.ResultDTO;
import com.mcmp.o11ymanager.entity.HostEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.enums.AgentAction;
import com.mcmp.o11ymanager.enums.ResponseStatus;
import com.mcmp.o11ymanager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.event.AgentHistoryEvent;
import com.mcmp.o11ymanager.event.AgentHistoryFailEvent;
import com.mcmp.o11ymanager.exception.config.FileReadingException;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.infrastructure.util.ChaCha20Poly3105Util;
import com.mcmp.o11ymanager.infrastructure.util.CheckUtil;
import com.mcmp.o11ymanager.model.agentHealth.SshConnection;
import com.mcmp.o11ymanager.model.host.HostAgentTaskStatus;
import com.mcmp.o11ymanager.model.semaphore.Task;
import com.mcmp.o11ymanager.oldService.domain.OldSemaphoreDomainService;
import com.mcmp.o11ymanager.oldService.domain.interfaces.HostService;
import com.mcmp.o11ymanager.oldService.domain.interfaces.SshService;
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
@RequiredArgsConstructor
@Service
public class FluentBitFacadeService {

  private final FileFacadeService fileFacadeService;
  private final HostService hostService;
  private static final Lock agentTaskStatusLock = new ReentrantLock();
  private final RequestInfo requestInfo;
  private final OldSemaphoreDomainService oldSemaphoreDomainService;
  private final ApplicationEventPublisher event;
  private final SshService sshService;
  private final OldSchedulerFacadeService oldSchedulerFacadeService;
  private final FluentBitConfigFacadeService fluentBitConfigFacadeService;

  public void install(@NotBlank String hostId, @NotBlank String requestUserId,
      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    hostService.isIdleLogAgent(hostId);

    // 2. host 상태 업데이트
    hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.INSTALLING);

    // 3. 로컬 파일 확인
    HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);

    String configContent;
    try {
      configContent = fileFacadeService.readAgentConfigFile(hostConnectionInfo.getHostId(),
          Agent.FLUENT_BIT);
    } catch (FileReadingException e) {
      // 로컬에 파일이 없을 경우 생성
      HostDTO hostDTO = hostService.findById(hostId);
      fluentBitConfigFacadeService.initFluentbitConfig(hostConnectionInfo, hostDTO.getType(),
          hostDTO.getCredentialId(), hostDTO.getCloudService());

      // 다시 읽기
      configContent = fileFacadeService.readAgentConfigFile(hostConnectionInfo.getHostId(),
          Agent.FLUENT_BIT);
    }

    // 4. 전송(semaphore) - 설치 요청
    Task task = oldSemaphoreDomainService.install(hostConnectionInfo, SemaphoreInstallMethod.INSTALL,
        configContent, Agent.FLUENT_BIT,
        templateCount);

    // 5. task ID, task status 업데이트
    hostService.updateLogAgentTaskStatusAndTaskId(hostId, HostAgentTaskStatus.INSTALLING,
        String.valueOf(task.getId()));

    // 6. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .requestUserId(requestUserId)
        .hostId(hostId)
        .agentAction(AgentAction.LOG_AGENT_INSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);

    // 7. 스케줄러 등록
    oldSchedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), hostId,
        SemaphoreInstallMethod.INSTALL, Agent.FLUENT_BIT, requestUserId);
  }

  public void update(@NotBlank String hostId, @NotBlank String requestUserId,
                     @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    hostService.isIdleLogAgent(hostId);

    // 2. host 상태 업데이트
    hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.UPDATING);

    // 3. 전송(semaphore) - 업데이트 요청
    HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);
    Task task = oldSemaphoreDomainService.install(hostConnectionInfo, SemaphoreInstallMethod.UPDATE,
            null, Agent.FLUENT_BIT,
            templateCount);

    // 4. task ID, task status 업데이트
    hostService.updateLogAgentTaskStatusAndTaskId(hostId, HostAgentTaskStatus.UPDATING,
            String.valueOf(task.getId()));

    // 5. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .hostId(hostId)
            .agentAction(AgentAction.LOG_AGENT_UPDATE_STARTED)
            .reason("")
            .build();

    event.publishEvent(successEvent);

    // 6. 스케줄러 등록
    oldSchedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), hostId,
            SemaphoreInstallMethod.UPDATE, Agent.FLUENT_BIT, requestUserId);
  }

  public void uninstall(String hostId, int templateCount, String requestUserId) throws Exception {

    // 1) 상태 확인
    hostService.isIdleLogAgent(hostId);

    // 2) 상태 변경
    hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);

    // 3. 전송(semaphore) - 삭제 요청
    HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);

    Task task = oldSemaphoreDomainService.install(hostConnectionInfo, SemaphoreInstallMethod.UNINSTALL,
        null, Agent.FLUENT_BIT,
        templateCount);

    // 5. task ID, task status 업데이트
    hostService.updateLogAgentTaskStatusAndTaskId(hostId, HostAgentTaskStatus.UNINSTALLING,
        String.valueOf(task.getId()));

    // 5) 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .requestUserId(requestUserId)
        .hostId(hostId)
        .agentAction(AgentAction.LOG_AGENT_UNINSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);
    // 6) 스케줄러 등록
    oldSchedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), hostId,
        SemaphoreInstallMethod.UNINSTALL, Agent.FLUENT_BIT, requestUserId);

  }

  @Transactional
  public List<ResultDTO> enable(String[] ids, String requestUserId) {
    List<ResultDTO> results = new ArrayList<>();

    for (String id : CheckUtil.emptyIfNull(List.of(ids))) {

      try {
        agentTaskStatusLock.lock();

        HostEntity host;
        host = hostService.findById(id).toEntity();

        // 1. 싫행 상태 확인
        hostService.isIdleLogAgent(id);

        // 2. ENABLING 상태로 변경
        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.ENABLING);

        host.setPassword(ChaCha20Poly3105Util.decryptString(host.getPassword()));

        // 3, 활성화 실행
        SshConnection connection = sshService.getConnection(
            host.getIp(), host.getPort(), host.getUser(), host.getPassword());

        sshService.enableFluentBit(connection, host.getIp(), host.getPort(),
            host.getUser(), host.getPassword());

        // 4. 모든 작업 완료 후 상태 IDLE(실행 요청 enabling에서) 업데이트
        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        agentTaskStatusLock.unlock();

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .reason("")
            .agentAction(AgentAction.ENABLE_FLUENT_BIT)
            .hostId(id)
            .build();

        event.publishEvent(successEvent);

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());
      } catch (Exception e) {

        agentTaskStatusLock.unlock();

        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .reason(e.getMessage())
            .agentAction(AgentAction.ENABLE_FLUENT_BIT)
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

        // 1. 싫행 상태 확인
        hostService.isIdleLogAgent(id);

        // 2. DISABLING 상태로 변경
        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.DISABLING);

        host.setPassword(ChaCha20Poly3105Util.decryptString(host.getPassword()));

        // 3. 비활성화 실행
        SshConnection connection = sshService.getConnection(
            host.getIp(), host.getPort(), host.getUser(), host.getPassword());

        sshService.disableFluentBit(connection, host.getIp(), host.getPort(),
            host.getUser(), host.getPassword());

        // 4. 작업 완료 후 idle 변경
        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .hostId(id)
            .agentAction(AgentAction.DISABLE_FLUENT_BIT)
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

        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.DISABLE_FLUENT_BIT)
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

        // 1. 싫행 상태 확인
        hostService.isIdleLogAgent(id);

        // 2. RESTARTING 상태로 변경
        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.RESTARTING);

        host.setPassword(ChaCha20Poly3105Util.decryptString(host.getPassword()));

        // 3. restart 실행
        SshConnection connection = sshService.getConnection(
            host.getIp(), host.getPort(), host.getUser(), host.getPassword());

        sshService.restartFluentBit(connection, host.getIp(), host.getPort(),
            host.getUser(), host.getPassword());

        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.RESTART_FLUENT_BIT)
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

        hostService.updateLogAgentTaskStatus(id, HostAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.RESTART_FLUENT_BIT)
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
