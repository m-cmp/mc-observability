package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.target.ResDTO;
import com.mcmp.o11ymanager.dto.target.TargetRegisterDTO;
import com.mcmp.o11ymanager.entity.AccessInfoEntity;
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
import com.mcmp.o11ymanager.oldService.domain.interfaces.TargetService;
import com.mcmp.o11ymanager.service.SemaphoreDomainService;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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

  private static final Lock agentTaskStatusLock = new ReentrantLock();
  private final RequestInfo requestInfo;
  private final ApplicationEventPublisher event;
  private final SemaphoreDomainService semaphoreDomainService;
  private final FileFacadeService fileFacadeService;
  private final SshService sshService;
  private final SchedulerFacadeService schedulerFacadeService;
  private final TargetService targetService;
  private final TelegrafConfigFacadeService telegrafConfigFacadeService;


  public void install(@NotBlank String targetId,
      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    targetService.isIdleMonitoringAgent(targetId);

    // 2. host 상태 업데이트
    targetService.updateMonitoringAgentTaskStatus(targetId, TargetAgentTaskStatus.INSTALLING);

    // 3. 로컬 파일 확인
    TargetRegisterDTO target = targetService.getTargetInfo(targetId);

    TargetRegisterDTO.AccessInfoDTO accessInfo = target.getAccessInfo();

    String configContent;
    try {
      configContent = fileFacadeService.readAgentConfigFile(target.getName(),
          Agent.TELEGRAF);
    } catch (FileReadingException e) {
      // 로컬에 파일이 없을 경우 생성
      Optional<TargetEntity> targetDTO = targetService.findById(targetId);
      telegrafConfigFacadeService.initTelegrafConfig(targetDTO, null);

      // 다시 읽기
      configContent = fileFacadeService.readAgentConfigFile(target.getName(),
          Agent.TELEGRAF);
    }

    // 4. 전송(semaphore) - 설치 요청
    Task task = semaphoreDomainService.install(accessInfo, SemaphoreInstallMethod.INSTALL,
        configContent, Agent.TELEGRAF,
        templateCount);

    // 5. task ID, task status 업데이트
    targetService.updateMonitoringAgentTaskStatusAndTaskId(targetId, TargetAgentTaskStatus.INSTALLING,
        String.valueOf(task.getId()));

    // 6. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .hostId(targetId)
        .agentAction(AgentAction.MONITORING_AGENT_INSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);

    // 7. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), targetId,
        SemaphoreInstallMethod.INSTALL, Agent.TELEGRAF);
  }

  public void update(@NotBlank String targetId, @NotBlank String requestUserId,
                      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    targetService.isIdleMonitoringAgent(targetId);

    // 2. host 상태 업데이트
    targetService.updateMonitoringAgentTaskStatus(targetId, TargetAgentTaskStatus.UPDATING);

    // 3. 전송(semaphore) - 업데이트 요청
    TargetRegisterDTO.AccessInfoDTO accessInfoDTO = targetService.getAccessInfo(targetId);
    Task task = semaphoreDomainService.install(accessInfoDTO, SemaphoreInstallMethod.UPDATE,
        null, Agent.TELEGRAF,
        templateCount);

    // 4. task ID, task status 업데이트
    targetService.updateMonitoringAgentTaskStatusAndTaskId(targetId, TargetAgentTaskStatus.UPDATING,
            String.valueOf(task.getId()));

    // 5. 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .hostId(targetId)
            .agentAction(AgentAction.MONITORING_AGENT_UPDATE_STARTED)
            .reason("")
            .build();

    event.publishEvent(successEvent);

    // 6. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), targetId,
            SemaphoreInstallMethod.UPDATE, Agent.TELEGRAF);
  }

  public void uninstall(String targetId, int templateCount, String requestUserId) throws Exception {

    // 1) 상태 확인
    targetService.isIdleMonitoringAgent(targetId);

    // 2) 상태 변경
    targetService.updateMonitoringAgentTaskStatus(targetId, TargetAgentTaskStatus.PREPARING);

    // 3) 전송(semaphore) - 삭제 요청
    TargetRegisterDTO.AccessInfoDTO accessInfoDTO = targetService.getAccessInfo(targetId);

    Task task = semaphoreDomainService.install(accessInfoDTO, SemaphoreInstallMethod.UNINSTALL,
        null, Agent.TELEGRAF,
        templateCount);

    // 4) task ID, task status 업데이트
    targetService.updateMonitoringAgentTaskStatusAndTaskId(targetId, TargetAgentTaskStatus.UNINSTALLING,
        String.valueOf(task.getId()));

    // 5) 이력 남기기
    AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
        .requestId(requestInfo.getRequestId())
        .requestUserId(requestUserId)
        .hostId(targetId)
        .agentAction(AgentAction.MONITORING_AGENT_UNINSTALL_STARTED)
        .reason("")
        .build();

    event.publishEvent(successEvent);


    // 6) 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), targetId,
        SemaphoreInstallMethod.UNINSTALL, Agent.TELEGRAF);
  }



  @Transactional
  public List<ResDTO> enable(String[] ids, String requestUserId) {
    List<ResDTO> results = new ArrayList<>();

    for (String id : CheckUtil.emptyIfNull(List.of(ids))) {
      try {
        agentTaskStatusLock.lock();

        Optional<TargetEntity> target = targetService.findById(id);
        AccessInfoEntity accessInfo = target.get().getAccessInfo();

        targetService.isIdleMonitoringAgent(id);
        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.ENABLING);

        // TODO : ssh key encoding, encryption
        String decryptedPassword = ChaCha20Poly3105Util.decryptString(accessInfo.getSshKey());

        SshConnection connection = sshService.getConnection(
            accessInfo.getIp(), accessInfo.getPort(), accessInfo.getUser(), decryptedPassword
        );

        sshService.enableTelegraf(
            connection,
            accessInfo.getIp(),
            accessInfo.getPort(),
            accessInfo.getUser(),
            decryptedPassword
        );

        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .reason("")
            .agentAction(AgentAction.ENABLE_TELEGRAF)
            .hostId(id)
            .build();

        event.publishEvent(successEvent);

        results.add(ResDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());

      } catch (Exception e) {
        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .reason(e.getMessage())
            .agentAction(AgentAction.ENABLE_TELEGRAF)
            .hostId(id)
            .build();

        event.publishEvent(failEvent);

        results.add(ResDTO.builder()
            .id(id)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());

      } finally {
        agentTaskStatusLock.unlock();
      }
    }

    return results;
  }


  @Transactional
  public List<ResDTO> disable(String[] ids, String requestUserId) {

    List<ResDTO> results = new ArrayList<>();

    for (String id : CheckUtil.emptyIfNull(List.of(ids))) {

      try {
        agentTaskStatusLock.lock();

        Optional<TargetEntity> target = targetService.findById(id);
        AccessInfoEntity accessInfo = target.get().getAccessInfo();

        targetService.isIdleMonitoringAgent(id);
        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.ENABLING);

        // TODO : ssh key encoding, encryption
        String decryptedPassword = ChaCha20Poly3105Util.decryptString(accessInfo.getSshKey());

        SshConnection connection = sshService.getConnection(
            accessInfo.getIp(), accessInfo.getPort(), accessInfo.getUser(), decryptedPassword
        );

        sshService.enableTelegraf(
            connection,
            accessInfo.getIp(),
            accessInfo.getPort(),
            accessInfo.getUser(),
            decryptedPassword
        );

        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .hostId(id)
            .agentAction(AgentAction.DISABLE_TELEGRAF)
            .reason("")
            .build();
        event.publishEvent(successEvent);

        results.add(ResDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());

        agentTaskStatusLock.unlock();
      } catch (Exception e) {

        agentTaskStatusLock.unlock();

        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.DISABLE_TELEGRAF)
            .reason(e.getMessage())
            .hostId(id)
            .build();
        event.publishEvent(failEvent);

        results.add(ResDTO.builder()
            .id(id)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      }
    }
    return results;
  }

  @Transactional
  public List<ResDTO> restart(String[] ids, String requestUserId) {

    List<ResDTO> results = new ArrayList<>();

    for (String id : CheckUtil.emptyIfNull(List.of(ids))) {

      try {
        agentTaskStatusLock.lock();

        Optional<TargetEntity> target = targetService.findById(id);
        AccessInfoEntity accessInfo = target.get().getAccessInfo();

        targetService.isIdleMonitoringAgent(id);
        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.ENABLING);

        // TODO : ssh key encoding, encryption
        String decryptedPassword = ChaCha20Poly3105Util.decryptString(accessInfo.getSshKey());

        SshConnection connection = sshService.getConnection(
            accessInfo.getIp(), accessInfo.getPort(), accessInfo.getUser(), decryptedPassword
        );

        sshService.enableTelegraf(
            connection,
            accessInfo.getIp(),
            accessInfo.getPort(),
            accessInfo.getUser(),
            decryptedPassword
        );

        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.IDLE);

        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.RESTART_TELEGRAF)
            .hostId(id)
            .reason("")
            .build();

        event.publishEvent(successEvent);

        results.add(ResDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());
        agentTaskStatusLock.unlock();
      } catch (Exception e) {
        agentTaskStatusLock.unlock();

        targetService.updateMonitoringAgentTaskStatus(id, TargetAgentTaskStatus.IDLE);

        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .requestUserId(requestUserId)
            .agentAction(AgentAction.RESTART_TELEGRAF)
            .reason(e.getMessage())
            .hostId(id)
            .build();

        event.publishEvent(failEvent);

        results.add(ResDTO.builder()
            .id(id)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      }
    }
    return results;
  }


}
