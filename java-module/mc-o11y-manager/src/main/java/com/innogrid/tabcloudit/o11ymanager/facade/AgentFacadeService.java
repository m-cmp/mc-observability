package com.innogrid.tabcloudit.o11ymanager.facade;

import com.innogrid.tabcloudit.o11ymanager.dto.host.AgentDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ConfigDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.HostConnectionDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.host.ResultDTO;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.enums.AgentAction;
import com.innogrid.tabcloudit.o11ymanager.enums.ResponseStatus;
import com.innogrid.tabcloudit.o11ymanager.enums.SemaphoreInstallMethod;
import com.innogrid.tabcloudit.o11ymanager.event.AgentHistoryEvent;
import com.innogrid.tabcloudit.o11ymanager.event.AgentHistoryFailEvent;
import com.innogrid.tabcloudit.o11ymanager.exception.host.*;
import com.innogrid.tabcloudit.o11ymanager.global.annotation.Base64Decode;
import com.innogrid.tabcloudit.o11ymanager.global.aspect.request.RequestInfo;
import com.innogrid.tabcloudit.o11ymanager.global.definition.ConfigDefinition;
import com.innogrid.tabcloudit.o11ymanager.infrastructure.util.CheckUtil;
import com.innogrid.tabcloudit.o11ymanager.model.agentHealth.SshConnection;
import com.innogrid.tabcloudit.o11ymanager.model.host.HostAgentTaskStatus;
import com.innogrid.tabcloudit.o11ymanager.model.semaphore.Task;
import com.innogrid.tabcloudit.o11ymanager.service.domain.SemaphoreDomainService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.FileService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.GitService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.HostService;
import com.innogrid.tabcloudit.o11ymanager.service.interfaces.SshService;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.innogrid.tabcloudit.o11ymanager.service.domain.SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS;

@Slf4j
@RequiredArgsConstructor
@Service
public class AgentFacadeService {

  private final HostService hostService;
  private final RequestInfo requestInfo;

  private static final Lock semaphoreInstallTemplateCurrentCountLock = new ReentrantLock();
  private static final Lock semaphoreConfigUpdateTemplateCurrentCountLock = new ReentrantLock();

  private final TelegrafConfigFacadeService telegrafConfigFacadeService;
  private final FluentBitConfigFacadeService fluentBitConfigFacadeService;
  private final GitService gitService;
  private final SemaphoreDomainService semaphoreDomainService;
  private final FileService fileService;
  private final GitFacadeService gitFacadeService;
  private final FileFacadeService fileFacadeService;
  private int semaphoreInstallTemplateCurrentCount = 0;
  private int semaphoreConfigUpdateTemplateCurrentCount = 0;

  private final ApplicationEventPublisher event;

  private final FluentBitFacadeService fluentBitFacadeService;
  private final TelegrafFacadeService telegrafFacadeService;
  private final SchedulerFacadeService schedulerFacadeService;
  private final SshService sshService;

  private final ConcurrentHashMap<String, ReentrantLock> repositoryLocks = new ConcurrentHashMap<>();

  private ReentrantLock getAgentLock(String uuid, Agent agent) {
    String lockKey = uuid + "-" + agent.name();
    return repositoryLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
  }

  @Transactional
  public List<ResultDTO> install(AgentDTO request, String requestUserId) {
    List<ResultDTO> results = new ArrayList<>();

    List<String> ids = List.of(request.getHost_id_list());

    if (!request.isSelectMonitoringAgent() && !request.isSelectLogAgent()) {
      throw new BadRequestException(requestInfo.getRequestId(), null, null, "에이전트가 선택되지 않았습니다!");
    }
    for (String id : CheckUtil.emptyIfNull(ids)) {
      ReentrantLock monitoringLock = getAgentLock(id, Agent.TELEGRAF);
      ReentrantLock loggingLock = getAgentLock(id, Agent.FLUENT_BIT);

      try {
        if (!hostService.existsById(id)) {
          throw new HostNotExistException(requestInfo.getRequestId(), String.join(", ", id));
        }

        // 1) Lock 걸기
        int templateCount;
        try {
          semaphoreInstallTemplateCurrentCountLock.lock();
          templateCount = getSemaphoreInstallTemplateCurrentCount();
        } finally {
          semaphoreInstallTemplateCurrentCountLock.unlock();
        }

        // 2 ) 에이전트 설치
        // 2-1 ) Telegraf 설치
        monitoringLock.lock();
        if (request.isSelectMonitoringAgent()) {
          telegrafFacadeService.install(id, requestUserId, templateCount);
        }

        // 2-1 ) FluentBit 설치
        loggingLock.lock();
        if (request.isSelectLogAgent()) {
          fluentBitFacadeService.install(id, requestUserId, templateCount);
        }

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());
      } catch (Exception e) {

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        loggingLock.unlock();
        monitoringLock.unlock();
      }
    }

    return results;
  }

  @Transactional
  public List<ResultDTO> update(AgentDTO request, String requestUserId) {

    List<ResultDTO> results = new ArrayList<>();

    List<String> ids = List.of(request.getHost_id_list());

    if (!request.isSelectMonitoringAgent() && !request.isSelectLogAgent()) {
      throw new BadRequestException(requestInfo.getRequestId(), null, null, "에이전트가 선택되지 않았습니다!");
    }
    for (String id : CheckUtil.emptyIfNull(ids)) {
      ReentrantLock monitoringLock = getAgentLock(id, Agent.TELEGRAF);
      ReentrantLock loggingLock = getAgentLock(id, Agent.FLUENT_BIT);

      try {
        if (!hostService.existsById(id)) {
          throw new HostNotExistException(requestInfo.getRequestId(), String.join(", ", id));
        }

        // 1) Lock 걸기
        int templateCount;
        try {
          semaphoreInstallTemplateCurrentCountLock.lock();
          templateCount = getSemaphoreInstallTemplateCurrentCount();
        } finally {
          semaphoreInstallTemplateCurrentCountLock.unlock();
        }

        // 2 ) 에이전트 업데이트
        // 2-1 ) Telegraf 업데이트
        monitoringLock.lock();
        if (request.isSelectMonitoringAgent()) {
          telegrafFacadeService.update(id, requestUserId, templateCount);
        }

        // 2-1 ) FluentBit 업데이트
        loggingLock.lock();
        if (request.isSelectLogAgent()) {
          fluentBitFacadeService.update(id, requestUserId, templateCount);
        }

        results.add(ResultDTO.builder()
                .id(id)
                .status(ResponseStatus.SUCCESS)
                .build());
      } catch (Exception e) {

        results.add(ResultDTO.builder()
                .id(id)
                .status(ResponseStatus.ERROR)
                .errorMessage(e.getMessage())
                .build());
      } finally {
        loggingLock.unlock();
        monitoringLock.unlock();
      }
    }

    return results;
  }

  private int getSemaphoreInstallTemplateCurrentCount() {
    if (semaphoreInstallTemplateCurrentCount > SEMAPHORE_MAX_PARALLEL_TASKS) {
      semaphoreInstallTemplateCurrentCount = 0;
    }
    semaphoreInstallTemplateCurrentCount++;

    return semaphoreInstallTemplateCurrentCount;
  }

  private int getSemaphoreConfigUpdateTemplateCurrentCount() {
    if (semaphoreConfigUpdateTemplateCurrentCount > SEMAPHORE_MAX_PARALLEL_TASKS) {
      semaphoreConfigUpdateTemplateCurrentCount = 0;
    }
    semaphoreConfigUpdateTemplateCurrentCount++;

    return semaphoreConfigUpdateTemplateCurrentCount;
  }


  @Transactional
  @Base64Decode(ConfigDTO.class)
  public List<ResultDTO> uninstall(String requestId, AgentDTO request, String requestUserId) {

    String hostId = null;
    List<ResultDTO> results = new ArrayList<>();

    List<String> ids = List.of(request.getHost_id_list());

    // 1) 삭제할 에이전트 선택되었는지 확인
    if (!request.isSelectMonitoringAgent() && !request.isSelectLogAgent()) {
      throw new BadRequestException(requestId, null, null, "에이전트가 선택되지 않았습니다!");
    }

    for (String id : CheckUtil.emptyIfNull(ids)) {
      ReentrantLock monitoringLock = getAgentLock(id, Agent.TELEGRAF);
      ReentrantLock loggingLock = getAgentLock(id, Agent.FLUENT_BIT);

      try {
        hostId = id;
        int templateCount;
        try {
          semaphoreInstallTemplateCurrentCountLock.lock();
          templateCount = getSemaphoreInstallTemplateCurrentCount();
        } finally {
          semaphoreInstallTemplateCurrentCountLock.unlock();
        }

        // 4 ) 에이전트 제거
        // 4-1 ) Telegraf 제거
        monitoringLock.lock();
        if (request.isSelectMonitoringAgent()) {
          telegrafFacadeService.uninstall(id, templateCount, requestUserId);
        }

        // 4-1 ) FluentBit 제거
        loggingLock.lock();
        if (request.isSelectLogAgent()) {
          fluentBitFacadeService.uninstall(id, templateCount, requestUserId);
        }

        results.add(ResultDTO.builder()
            .id(id)
            .status(ResponseStatus.SUCCESS)
            .build());
      } catch (Exception e) {
        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());

      } finally {
        loggingLock.unlock();
        monitoringLock.unlock();
      }
    }

    return results;
  }


  @Transactional
  @Base64Decode(ConfigDTO.class)
  public List<ResultDTO> updateTelegrafConfig(String requestId, AgentDTO request,
      ConfigDTO configDTO, String requestUserId) {

    // 1) 에이전트 설치 종류 확인
    if (!request.isSelectMonitoringAgent() && !request.isSelectLogAgent()) {
      throw new BadRequestException(requestId, null, null, "에이전트가 선택되지 않았습니다!");
    }

    List<String> ids = List.of(request.getHost_id_list());
    List<ResultDTO> results = new ArrayList<>();

    for (String hostId : CheckUtil.emptyIfNull(ids)) {
      ReentrantLock monitoringLock = getAgentLock(hostId, Agent.TELEGRAF);

      try {
        monitoringLock.lock();

        // 2) 호스트 상태 확인
        hostService.isIdleMonitoringAgent(hostId);
        // 3) 에이전트 상태 확인
        hostService.isMonitoringAgentInstalled(hostId);

        // 4) 템플릿 카운트
        int templateCount;
        semaphoreConfigUpdateTemplateCurrentCountLock.lock();
        try {
          templateCount = getSemaphoreConfigUpdateTemplateCurrentCount();
        } finally {
          semaphoreConfigUpdateTemplateCurrentCountLock.unlock();
        }

        // 5) 호스트 상태 변경
        hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);

        // 6) 파일 수정
        telegrafConfigFacadeService.updateTelegrafConfig(hostId, configDTO.getContent(), configDTO.getPath());

        // 7) Git 커밋
        Path telegrafConfigWorkingPath = telegrafConfigFacadeService.getTelegrafConfigWorkingPath(
            hostId);
        Git git = gitService.getGit(telegrafConfigWorkingPath.toFile());
        gitService.commit(git, ".", "Config updated (" + configDTO.getPath() + ")",
            "innogrid",
            "cmp@innogrid.com");

        // 8) DB에 Git Hash 값 수정
        String commitHash = gitService.getHashName(git);
        hostService.updateMonitoringAgentConfigGitHash(hostId, commitHash);

        // 9) Semaphore 수정 요청
        Task task;
        HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);
        String remoteConfigPath =
            fileFacadeService.getHostConfigTelegrafRemotePath() + "/" + configDTO.getPath();

        task = semaphoreDomainService.updateConfig(hostConnectionInfo, remoteConfigPath,
            configDTO.getContent(), Agent.TELEGRAF, templateCount);

        Integer taskId = null;
        if (task != null) {
          taskId = task.getId();
        }

        // 10) 호스트 상태 변경
        hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.UPDATING_CONFIG);

        // 11) 액션 기록
        AgentHistoryEvent successsEvent = AgentHistoryEvent.builder()
            .requestId(requestId)
            .agentAction(AgentAction.MONITORING_AGENT_CONFIG_UPDATE_STARTED)
            .hostId(hostId)
            .requestUserId(requestUserId)
            .reason("")
            .build();

        event.publishEvent(successsEvent);

        // 12) 스케줄러 등록
        schedulerFacadeService.scheduleTaskStatusCheck(requestId, taskId, hostId,
            SemaphoreInstallMethod.CONFIG_UPDATE, Agent.TELEGRAF, requestUserId);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.SUCCESS)
            .build());

      } catch (Exception e) {
        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .agentAction(AgentAction.MONITORING_AGENT_CONFIG_UPDATE_FAILED)
            .hostId(hostId)
            .requestId(requestId)
            .requestUserId(requestUserId)
            .reason(e.getMessage())
            .build();

        event.publishEvent(failEvent);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        monitoringLock.unlock();
      }
    }

    return results;
  }


  @Transactional
  @Base64Decode(ConfigDTO.class)
  public List<ResultDTO> updateFluentbitConfig(String requestId, AgentDTO request,
      ConfigDTO configDTO, String requestUserId) {

    // 1) 에이전트 설치 종류 확인
    if (!request.isSelectLogAgent()) {
      throw new BadRequestException(requestId, null, null, "에이전트가 선택되지 않았습니다!");
    }

    List<String> ids = List.of(request.getHost_id_list());
    List<ResultDTO> results = new ArrayList<>();

    for (String hostId : CheckUtil.emptyIfNull(ids)) {
      ReentrantLock loggingLock = getAgentLock(hostId, Agent.FLUENT_BIT);

      try {
        loggingLock.lock();

        // 2) 호스트 상태 확인
        hostService.isIdleLogAgent(hostId);
        // 3) 에이전트 상태 확인
        hostService.isLogAgentInstalled(hostId);

        // 4) 템플릿 카운트
        int templateCount;
        try {
          semaphoreConfigUpdateTemplateCurrentCountLock.lock();
          templateCount = getSemaphoreConfigUpdateTemplateCurrentCount();
        } finally {
          semaphoreConfigUpdateTemplateCurrentCountLock.unlock();
        }

        // 5) 호스트 상태 변경
        hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);

        // 6) 파일 수정
        fluentBitConfigFacadeService.updateFluentBitConfig(hostId, configDTO.getContent(), configDTO.getPath());

        // 7) Git 커밋
        Path fluentbitConfigWorkingPath = fluentBitConfigFacadeService.getFluentbitConfigWorkingPath(
            hostId);
        Git git = gitService.getGit(fluentbitConfigWorkingPath.toFile());
        gitService.commit(git, ".", "Config updated (" + configDTO.getPath() + ")",
            "innogrid",
            "cmp@innogrid.com");

        // 8) DB에 Git Hash 값 수정
        String commitHash = gitService.getHashName(git);
        hostService.updateLogAgentConfigGitHash(hostId, commitHash);

        // 9) Semaphore 수정 요청
        Task task;
        HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);
        String remoteConfigPath =
            fileFacadeService.getHostConfigFluentBitRemotePath() + "/" + configDTO.getPath();

        task = semaphoreDomainService.updateConfig(hostConnectionInfo, remoteConfigPath,
            configDTO.getContent(), Agent.FLUENT_BIT, templateCount);

        Integer taskId = null;
        if (task != null) {
          taskId = task.getId();
        }

        // 10) 호스트 상태 변경
        hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.UPDATING_CONFIG);

        // 11) 액션 기록
        AgentHistoryEvent successsEvent = AgentHistoryEvent.builder()
                .requestId(requestId)
                .agentAction(AgentAction.LOG_AGENT_CONFIG_UPDATE_STARTED)
                .hostId(hostId)
                .requestUserId(requestUserId)
                .reason("")
                .build();

        // 12) 스케줄러 등록
        schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), taskId, hostId,
            SemaphoreInstallMethod.CONFIG_UPDATE, Agent.FLUENT_BIT, requestUserId);

        event.publishEvent(successsEvent);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.SUCCESS)
            .build());
      } catch (Exception e) {
        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestId)
            .agentAction(AgentAction.LOG_AGENT_CONFIG_UPDATE_FAILED)
            .hostId(hostId)
            .requestUserId(requestUserId)
            .reason(e.getMessage())
            .build();

        event.publishEvent(failEvent);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        loggingLock.unlock();
      }
    }

    return results;
  }


  @Transactional
  @Base64Decode(ConfigDTO.class)
  public List<ResultDTO> rollbackTelegrafConfig(List<String> hostIds, String commitHash,
      String requestUserId) {

    List<ResultDTO> results = new ArrayList<>();

    for (String hostId : CheckUtil.emptyIfNull(hostIds)) {
      ReentrantLock monitoringLock = getAgentLock(hostId, Agent.TELEGRAF);

      try {
        monitoringLock.lock();

        log.info("[RUN}====================Start rollback telegraf config====================");

        // 1) 호스트 상태 확인
        hostService.isIdleMonitoringAgent(hostId);
        // 2) 에이전트 상태 확인
        hostService.isMonitoringAgentInstalled(hostId);

        // 3) 상태 변경
        hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);

        // 4) Rollback 실행
        hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.ROLLING_BACK_CONFIG);
        Path telegrafWorkingPath = telegrafConfigFacadeService.getTelegrafConfigWorkingPath(hostId);

        String telegrafConfContents = gitFacadeService.getFileContentOfCommitHash(
            requestInfo.getRequestId(), hostId,
            Agent.TELEGRAF, commitHash, ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG);
        log.info("Telegraf Commit Hash : {}", commitHash);
        fileService.writeFile(telegrafWorkingPath.toFile(),
            ConfigDefinition.HOST_CONFIG_NAME_TELEGRAF_MAIN_CONFIG,
            telegrafConfContents);

        // 5) Git commit
        Git git = gitService.getGit(telegrafWorkingPath.toFile());
        log.info("telegrafWorkingPath : {}", telegrafWorkingPath);
        String commitMessage = "Rolled back config using commit hash: " + commitHash;

        gitService.commit(git, ".", commitMessage, "innogrid", "cmp@innogrid.com");

        // 6) SSH로 config 수정
        HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);
        fileFacadeService.sendConfigs(requestInfo.getRequestId(), hostConnectionInfo,
            Agent.TELEGRAF);

        // 7) Agent restart 요청
        SshConnection connection = sshService.getConnection(
            hostConnectionInfo.getIp(), hostConnectionInfo.getPort(),
            hostConnectionInfo.getUserId(),
            hostConnectionInfo.getPassword());

        sshService.restartTelegraf(connection, hostConnectionInfo.getIp(),
            hostConnectionInfo.getPort(), hostConnectionInfo.getUserId(),
            hostConnectionInfo.getPassword());

        // 8) GitHash 값 수정
        hostService.updateMonitoringAgentConfigGitHash(hostId, commitHash);

        // 9) 액션 기록
        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .agentAction(AgentAction.MONITORING_AGENT_CONFIG_ROLLBACK_STARTED)
            .requestUserId(requestUserId)
            .reason("")
            .hostId(hostId)
            .build();
        event.publishEvent(successEvent);

        log.info("[DONE}====================End rollback telegraf config====================");

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.SUCCESS)
            .build());
      } catch (Exception e) {
        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .agentAction(AgentAction.MONITORING_AGENT_CONFIG_ROLLBACK_FAILED)
            .hostId(hostId)
            .requestUserId(requestUserId)
            .reason(e.getMessage())
            .build();
        event.publishEvent(failEvent);

        hostService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        monitoringLock.unlock();
      }
    }
    return results;
  }


  @Transactional
  @Base64Decode(ConfigDTO.class)
  public List<ResultDTO> rollbackFluentbitConfig(List<String> hostIds, String
          commitHash,
      String requestUserId) {

    List<ResultDTO> results = new ArrayList<>();

    for (String hostId : CheckUtil.emptyIfNull(hostIds)) {
      ReentrantLock loggingLock = getAgentLock(hostId, Agent.FLUENT_BIT);

      try {
        loggingLock.lock();

        // 1) 호스트 상태 확인
        hostService.isIdleLogAgent(hostId);
        // 2) 에이전트 상태 확인
        hostService.isLogAgentInstalled(hostId);

        // 3) 상태 변경
        hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);

        // 4) Rollback 실행
        hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.ROLLING_BACK_CONFIG);
        Path fluentbitWorkingPath = fluentBitConfigFacadeService.getFluentbitConfigWorkingPath(
            hostId);

        String fluentbitConfContents = gitFacadeService.getFileContentOfCommitHash(
            requestInfo.getRequestId(), hostId,
            Agent.FLUENT_BIT, commitHash, ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES);

        fileService.writeFile(fluentbitWorkingPath.toFile(),
            ConfigDefinition.HOST_CONFIG_NAME_FLUENTBIT_VARIABLES,
            fluentbitConfContents);

        // 5) Git commit
        Git git = gitService.getGit(fluentbitWorkingPath.toFile());
        String commitMessage = "Rolled back config using commit hash: " + commitHash;

        gitService.commit(git, ".", commitMessage, "innogrid", "cmp@innogrid.com");

        // 6) SSH로 config 수정
        HostConnectionDTO hostConnectionInfo = hostService.getHostConnectionInfo(hostId);
        fileFacadeService.sendConfigs(requestInfo.getRequestId(), hostConnectionInfo,
            Agent.FLUENT_BIT);

        // 7) Agent restart 요청
        SshConnection connection = sshService.getConnection(
            hostConnectionInfo.getIp(), hostConnectionInfo.getPort(),
            hostConnectionInfo.getUserId(),
            hostConnectionInfo.getPassword());

        sshService.restartFluentBit(connection, hostConnectionInfo.getIp(),
            hostConnectionInfo.getPort(), hostConnectionInfo.getUserId(),
            hostConnectionInfo.getPassword());

        // 8) GitHash 값 수정
        hostService.updateLogAgentConfigGitHash(hostId, commitHash);

        // 9) 액션 기록
        AgentHistoryEvent successEvent = AgentHistoryEvent.builder()
            .requestId(requestInfo.getRequestId())
            .agentAction(AgentAction.LOG_AGENT_CONFIG_ROLLBACK_STARTED)
            .requestUserId(requestUserId)
            .reason("")
            .hostId(hostId)
            .build();
        event.publishEvent(successEvent);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.SUCCESS)
            .build());
      } catch (Exception e) {
        AgentHistoryFailEvent failEvent = AgentHistoryFailEvent.builder()
            .requestId(requestInfo.getRequestId())
            .agentAction(AgentAction.LOG_AGENT_CONFIG_ROLLBACK_FAILED)
            .hostId(hostId)
            .requestUserId(requestUserId)
            .reason(e.getMessage())
            .build();
        event.publishEvent(failEvent);

        hostService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.IDLE);

        results.add(ResultDTO.builder()
            .id(hostId)
            .status(ResponseStatus.ERROR)
            .errorMessage(e.getMessage())
            .build());
      } finally {
        loggingLock.unlock();
      }
    }

    return results;
  }
}
