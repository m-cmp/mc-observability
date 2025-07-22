package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.target.ResultDTO;
import com.mcmp.o11ymanager.entity.TargetEntity;
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
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.semaphore.Task;
import com.mcmp.o11ymanager.oldService.domain.interfaces.SshService;
import com.mcmp.o11ymanager.service.SemaphoreDomainService;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
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

  private final TargetService targetService;
  private static final Lock agentTaskStatusLock = new ReentrantLock();
  private final RequestInfo requestInfo;
  private final ApplicationEventPublisher event;
  private final SemaphoreDomainService semaphoreDomainService;
  private final FileFacadeService fileFacadeService;
  private final SshService sshService;
  private final SchedulerFacadeService schedulerFacadeService;
  private final TelegrafConfigFacadeService telegrafConfigFacadeService;

  public void install(String nsId, String mciId, String targetId,
      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    targetService.isIdleMonitoringAgent(nsId, mciId, targetId);

    // 2. host 상태 업데이트
    targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.INSTALLING);

    String configContent = telegrafConfigFacadeService.initTelegrafConfig(nsId, mciId, targetId);

    // 4. 전송(semaphore) - 설치 요청
    Task task = semaphoreDomainService.install(nsId, mciId, targetId, SemaphoreInstallMethod.INSTALL,
        configContent, Agent.TELEGRAF,
        templateCount);

    // 5. task ID, task status 업데이트
    targetService.updateMonitoringAgentTaskStatusAndTaskId(nsId, mciId, targetId, TargetAgentTaskStatus.INSTALLING,
        String.valueOf(task.getId()));

    // 6. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .nsId(nsId)
        .mciId(mciId)
        .targetId(targetId)
        .agentAction(AgentAction.MONITORING_AGENT_INSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);

    // 7. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), nsId, mciId, targetId,
        SemaphoreInstallMethod.INSTALL, Agent.TELEGRAF);
  }

  public void update(String nsId, String mciId, String targetId,
                      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    targetService.isIdleMonitoringAgent(nsId, mciId, targetId);

    // 2. host 상태 업데이트
    targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.UPDATING);

    // 3. 전송(semaphore) - 업데이트 요청
    Task task = semaphoreDomainService.install(nsId, mciId, targetId, SemaphoreInstallMethod.UPDATE,
            null, Agent.TELEGRAF,
            templateCount);

    // 4. task ID, task status 업데이트
    targetService.updateMonitoringAgentTaskStatusAndTaskId(nsId, mciId, targetId, TargetAgentTaskStatus.UPDATING,
            String.valueOf(task.getId()));

    // 5. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .nsId(nsId)
            .mciId(mciId)
            .targetId(targetId)
            .agentAction(AgentAction.MONITORING_AGENT_UPDATE_STARTED)
            .reason("")
            .build();

    event.publishEvent(successEvent);

    // 6. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), nsId, mciId, targetId,
            SemaphoreInstallMethod.UPDATE, Agent.TELEGRAF);
  }

  public void uninstall(String nsId, String mciId, String targetId, int templateCount) throws Exception {

    // 1) 상태 확인
    targetService.isIdleMonitoringAgent(nsId, mciId, targetId);

    // 2) 상태 변경
    targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.PREPARING);

    // 3) 전송(semaphore) - 삭제 요청
    Task task = semaphoreDomainService.install(nsId, mciId, targetId, SemaphoreInstallMethod.UNINSTALL,
        null, Agent.TELEGRAF,
        templateCount);

    // 4) task ID, task status 업데이트
    targetService.updateMonitoringAgentTaskStatusAndTaskId(nsId, mciId, targetId, TargetAgentTaskStatus.UNINSTALLING,
        String.valueOf(task.getId()));

    // 5) 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .nsId(nsId)
        .mciId(mciId)
        .targetId(targetId)
        .agentAction(AgentAction.MONITORING_AGENT_UNINSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);


    // 6) 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), nsId, mciId, targetId,
        SemaphoreInstallMethod.UNINSTALL, Agent.TELEGRAF);
  }


  @Transactional
  public List<ResultDTO> enable(String nsId, String mciId, String targetId, String requestUserId) {
    List<ResultDTO> results = new ArrayList<>();

    try {
      agentTaskStatusLock.lock();

      TargetEntity target;
      target = targetService.get(nsId, mciId, targetId).toEntity();

      targetService.isIdleMonitoringAgent(nsId, mciId, targetId);

      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.ENABLING);

      // TODO : Use Tumblebug CMD
//      SshConnection connection = sshService.getConnection(
//              target.getIp(), target.getPort(), target.getUser(), target.getPassword());
//
//      sshService.enableTelegraf(connection, target.getIp(), target.getPort(),
//              target.getUser(), target.getPassword());

      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

      AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
              .requestId(requestInfo.getRequestId())
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .agentAction(AgentAction.ENABLE_FLUENT_BIT)
              .reason("")
              .build();

      event.publishEvent(successEvent);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());

      agentTaskStatusLock.unlock();
    } catch (Exception e) {

      agentTaskStatusLock.unlock();

      targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.IDLE);

      AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
              .requestId(requestInfo.getRequestId())
              .reason(e.getMessage())
              .agentAction(AgentAction.ENABLE_TELEGRAF)
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .build();

      event.publishEvent(failEvent);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.ERROR)
              .errorMessage(e.getMessage())
              .build());
    }
    
    return results;
  }

  @Transactional
  public List<ResultDTO> disable(String nsId, String mciId, String targetId, String requestUserId) {
    List<ResultDTO> results = new ArrayList<>();

    try {
      agentTaskStatusLock.lock();

      TargetEntity target;
      target = targetService.get(nsId, mciId, targetId).toEntity();

      targetService.isIdleMonitoringAgent(nsId, mciId, targetId);

      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.DISABLING);

      // TODO : Use Tumblebug CMD
//      SshConnection connection = sshService.getConnection(
//              target.getIp(), target.getPort(), target.getUser(), target.getPassword());
//
//      sshService.disableTelgraf(connection, target.getIp(), target.getPort(),
//              target.getUser(), target.getPassword());

      // 4. 작업 완료 후 idle 변경
      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

      AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
              .requestId(requestInfo.getRequestId())
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .agentAction(AgentAction.DISABLE_TELEGRAF)
              .reason("")
              .build();
      event.publishEvent(successEvent);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());

      agentTaskStatusLock.unlock();
    } catch (Exception e) {

      agentTaskStatusLock.unlock();

      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

      AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
              .requestId(requestInfo.getRequestId())
              .reason(e.getMessage())
              .agentAction(AgentAction.DISABLE_TELEGRAF)
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .build();
      event.publishEvent(failEvent);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.ERROR)
              .errorMessage(e.getMessage())
              .build());
    }

    return results;
  }

  @Transactional
  public List<ResultDTO> restart(String nsId, String mciId, String targetId, String requestUserId) {

    List<ResultDTO> results = new ArrayList<>();

    try {
      agentTaskStatusLock.lock();

      TargetEntity target;

      target = targetService.get(nsId, mciId, targetId).toEntity();

      targetService.isIdleMonitoringAgent(nsId, mciId, targetId);

      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.RESTARTING);

      // TODO : Use Tumblebug CMD
//      SshConnection connection = sshService.getConnection(
//              target.getIp(), target.getPort(), target.getUser(), target.getPassword());
//
//      sshService.restartTelegraf(connection, target.getIp(), target.getPort(),
//              target.getUser(), target.getPassword());

      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

      AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
              .requestId(requestInfo.getRequestId())
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .reason("")
              .build();

      event.publishEvent(successEvent);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());
      agentTaskStatusLock.unlock();
    } catch (Exception e) {
      agentTaskStatusLock.unlock();

      targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

      AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
              .requestId(requestInfo.getRequestId())
              .reason(e.getMessage())
              .agentAction(AgentAction.ENABLE_FLUENT_BIT)
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .build();

      event.publishEvent(failEvent);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.ERROR)
              .errorMessage(e.getMessage())
              .build());
    }

    return results;
  }


}
