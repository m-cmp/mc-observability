package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.host.ConfigDTO;
import com.mcmp.o11ymanager.dto.target.ResultDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.enums.ResponseStatus;
import com.mcmp.o11ymanager.global.annotation.Base64Decode;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.service.domain.SemaphoreDomainService;
import com.mcmp.o11ymanager.service.interfaces.TargetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.mcmp.o11ymanager.service.domain.SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS;

@Slf4j
@RequiredArgsConstructor
@Service
public class AgentFacadeService {

  private final TargetService targetService;
  private final RequestInfo requestInfo;

  private static final Lock semaphoreInstallTemplateCurrentCountLock = new ReentrantLock();
  private static final Lock semaphoreConfigUpdateTemplateCurrentCountLock = new ReentrantLock();

  private final TelegrafConfigFacadeService telegrafConfigFacadeService;
  private final FluentBitConfigFacadeService fluentBitConfigFacadeService;
  private final SemaphoreDomainService semaphoreDomainService;
  private final FileFacadeService fileFacadeService;
  private int semaphoreInstallTemplateCurrentCount = 0;
  private int semaphoreConfigUpdateTemplateCurrentCount = 0;


  private final FluentBitFacadeService fluentBitFacadeService;
  private final TelegrafFacadeService telegrafFacadeService;
  private final SchedulerFacadeService schedulerFacadeService;
  private final ConcurrentHashMap<String, ReentrantLock> repositoryLocks = new ConcurrentHashMap<>();

  private ReentrantLock getAgentLock(String nsId, String mciId, String targetId) {
    String lockKey = nsId + "-" + mciId + "-" + targetId;
    return repositoryLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
  }


  @Transactional
  public List<ResultDTO> install(String nsId, String mciId, String targetId) {
    List<ResultDTO> results = new ArrayList<>();
    ReentrantLock agentLock = getAgentLock(nsId, mciId, targetId);
    boolean lockAcquired = false;

    try {
      TargetDTO targetDTO = targetService.get(nsId, mciId, targetId);

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
      agentLock.lock();
      lockAcquired = true;

      telegrafFacadeService.install(nsId, mciId, targetId, templateCount);

      // 2-2 ) FluentBit 설치
      fluentBitFacadeService.install(nsId, mciId, targetId, templateCount);

      results.add(ResultDTO.builder()
          .nsId(nsId)
          .mciId(mciId)
          .targetId(targetId)
          .status(ResponseStatus.SUCCESS)
          .build());

    } catch (Exception e) {
      results.add(ResultDTO.builder()
          .nsId(nsId)
          .mciId(mciId)
          .targetId(targetId)
          .status(ResponseStatus.ERROR)
          .errorMessage(e.getMessage())
          .build());
    } finally {
      if (lockAcquired) {
        agentLock.unlock();
      }
    }


    return results;
  }

  @Transactional
  public List<ResultDTO> update(String nsId, String mciId, String targetId) {
    List<ResultDTO> results = new ArrayList<>();
    ReentrantLock agentLock = getAgentLock(nsId, mciId, targetId);

    try {
      TargetDTO targetDTO = targetService.get(nsId, mciId, targetId);

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
      agentLock.lock();
      telegrafFacadeService.update(nsId, mciId, targetId, templateCount);

      // 2-1 ) FluentBit 업데이트
      agentLock.lock();
      fluentBitFacadeService.update(nsId, mciId, targetId, templateCount);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());
    } catch (Exception e) {
      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.ERROR)
              .errorMessage(e.getMessage())
              .build());
    } finally {
      agentLock.unlock();
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
  public List<ResultDTO> uninstall(String nsId, String mciId, String targetId) {

    targetId = null;
    List<ResultDTO> results = new ArrayList<>();

    ReentrantLock agentLock = getAgentLock(nsId, mciId, targetId);

    String id = requestInfo.getRequestId();

    try {
      targetId = id;
      int templateCount;
      try {
        semaphoreInstallTemplateCurrentCountLock.lock();
        templateCount = getSemaphoreInstallTemplateCurrentCount();
      } finally {
        semaphoreInstallTemplateCurrentCountLock.unlock();
      }

      // 4 ) 에이전트 제거
      // 4-1 ) Telegraf 제거
      agentLock.lock();
      telegrafFacadeService.uninstall(nsId, mciId, targetId, templateCount );

      // 4-1 ) FluentBit 제거
      fluentBitFacadeService.uninstall(nsId, mciId, targetId, templateCount);

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());
    } catch (Exception e) {
      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.ERROR)
              .errorMessage(e.getMessage())
              .build());

    } finally {
      agentLock.unlock();
    }

    return results;
  }


//  @Transactional
//  @Base64Decode(ConfigDTO.class)
//  public List<ResultDTO> updateTelegrafConfig(ConfigDTO configDTO) {
//
//
//    String targetId = configDTO.getTargetId();
//    String nsId = configDTO.getNsId();
//    String mciId = configDTO.getMciId();
//
//    List<ResultDTO> results = new ArrayList<>();
//    // TODO : telegraf만 lock 거는 메소드 추가 필요한지
//      ReentrantLock monitoringLock = getAgentLock(nsId, mciId, targetId);
//
//      try {
//        monitoringLock.lock();
//
//        // 2) 호스트 상태 확인
//        targetService.isIdleMonitoringAgent(nsId, mciId, targetId);
//        // 3) 에이전트 상태 확인
//        targetService.isMonitoringAgentInstalled(nsId, mciId, targetId);
//
//        // 4) 템플릿 카운트
//        int templateCount;
//        semaphoreConfigUpdateTemplateCurrentCountLock.lock();
//        try {
//          templateCount = getSemaphoreConfigUpdateTemplateCurrentCount();
//        } finally {
//          semaphoreConfigUpdateTemplateCurrentCountLock.unlock();
//        }
//
//
//        // 6) 파일 수정
//
//        telegrafConfigFacadeService.updateTelegrafConfig(target, configDTO.getContent(),
//            configDTO.getPath());
//
//        // 7) Semaphore 수정 요청
//        Task task;
//        HostConnectionDTO hostConnectionInfo = targetService.getHostConnectionInfo(hostId);
//        String remoteConfigPath =
//            fileFacadeService.getHostConfigTelegrafRemotePath() + "/" + configDTO.getPath();
//
//        task = oldSemaphoreDomainService.updateConfig(hostConnectionInfo, remoteConfigPath,
//            configDTO.getContent(), Agent.TELEGRAF, templateCount);
//
//        Integer taskId = null;
//        if (task != null) {
//          taskId = task.getId();
//        }
//
//        // 8) 호스트 상태 변경
//        targetService.updateMonitoringAgentTaskStatus(hostId, HostAgentTaskStatus.UPDATING_CONFIG);
//
//
//        // 10) 스케줄러 등록
//        schedulerFacadeService.scheduleTaskStatusCheck(requestId, taskId, hostId,
//            SemaphoreInstallMethod.CONFIG_UPDATE, Agent.TELEGRAF, requestUserId);
//
//        results.add(ResultDTO.builder()
//            .id(hostId)
//            .status(ResponseStatus.SUCCESS)
//            .build());
//
//      } catch (Exception e) {
//
//        results.add(ResultDTO.builder()
//            .id(hostId)
//            .status(ResponseStatus.ERROR)
//            .errorMessage(e.getMessage())
//            .build());
//      } finally {
//        monitoringLock.unlock();
//      }
//    }
//
//    return results;
//  }


//  @Transactional
//  @Base64Decode(ConfigDTO.class)
//  public List<ResultDTO> updateFluentbitConfig(String requestId, AgentDTO request,
//      ConfigDTO configDTO, String requestUserId) {
//
//    // 1) 에이전트 설치 종류 확인
//    if (!request.isSelectLogAgent()) {
//      throw new BadRequestException(requestId, null, null, "에이전트가 선택되지 않았습니다!");
//    }
//
//    List<String> ids = List.of(request.getHost_id_list());
//    List<ResultDTO> results = new ArrayList<>();
//
//    for (String hostId : CheckUtil.emptyIfNull(ids)) {
//      ReentrantLock loggingLock = getAgentLock(hostId, Agent.FLUENT_BIT);
//
//      try {
//        loggingLock.lock();
//
//        // 2) 호스트 상태 확인
//        targetService.isIdleLogAgent(hostId);
//        // 3) 에이전트 상태 확인
//        targetService.isLogAgentInstalled(hostId);
//
//        // 4) 템플릿 카운트
//        int templateCount;
//        try {
//          semaphoreConfigUpdateTemplateCurrentCountLock.lock();
//          templateCount = getSemaphoreConfigUpdateTemplateCurrentCount();
//        } finally {
//          semaphoreConfigUpdateTemplateCurrentCountLock.unlock();
//        }
//
//        // 5) 호스트 상태 변경
//        targetService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);
//
//        // 6) 파일 수정
//        fluentBitConfigFacadeService.updateFluentBitConfig(hostId, configDTO.getContent(),
//            configDTO.getPath());
//
//        // 7) Semaphore 수정 요청
//        Task task;
//        HostConnectionDTO hostConnectionInfo = targetService.getHostConnectionInfo(hostId);
//        String remoteConfigPath =
//            fileFacadeService.getHostConfigFluentBitRemotePath() + "/" + configDTO.getPath();
//
//        task = oldSemaphoreDomainService.updateConfig(hostConnectionInfo, remoteConfigPath,
//            configDTO.getContent(), Agent.FLUENT_BIT, templateCount);
//
//        Integer taskId = null;
//        if (task != null) {
//          taskId = task.getId();
//        }
//
//        // 8) 호스트 상태 변경
//        targetService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.UPDATING_CONFIG);
//
//
//        // 10) 스케줄러 등록
//        schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), taskId, hostId,
//            SemaphoreInstallMethod.CONFIG_UPDATE, Agent.FLUENT_BIT, requestUserId);
//
//
//        results.add(ResultDTO.builder()
//            .id(hostId)
//            .status(ResponseStatus.SUCCESS)
//            .build());
//      } catch (Exception e) {
//
//        results.add(ResultDTO.builder()
//            .id(hostId)
//            .status(ResponseStatus.ERROR)
//            .errorMessage(e.getMessage())
//            .build());
//      } finally {
//        loggingLock.unlock();
//      }
//    }
//
//    return results;
//  }
}
