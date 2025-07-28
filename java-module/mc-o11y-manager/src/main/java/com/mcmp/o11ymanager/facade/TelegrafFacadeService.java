package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.target.ResultDTO;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.enums.ResponseStatus;
import com.mcmp.o11ymanager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.model.host.TargetAgentTaskStatus;
import com.mcmp.o11ymanager.model.semaphore.Task;
import com.mcmp.o11ymanager.service.domain.SemaphoreDomainService;
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
  private final SchedulerFacadeService schedulerFacadeService;
  private final TelegrafConfigFacadeService telegrafConfigFacadeService;

  public void install(String nsId, String mciId, String targetId,
      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    log.info("==========================telegraf idle start===============================");
    targetService.isIdleMonitoringAgent(nsId, mciId, targetId);
    log.info("==========================telegraf idle finish===============================");

    // 2. host 상태 업데이트
    targetService.updateMonitoringAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.INSTALLING);
    log.info("==========================update target status===============================");

    String configContent = telegrafConfigFacadeService.initTelegrafConfig(nsId, mciId, targetId);

    log.info(String.format("Telegraf config: %s", configContent));


    log.info("========================= START INSTALL REQUEST============================");
    // 4. 전송(semaphore) - 설치 요청
    Task task = semaphoreDomainService.install(nsId, mciId, targetId, SemaphoreInstallMethod.INSTALL,
        configContent, Agent.TELEGRAF,
        templateCount);

    log.info("=========================FINISH INSTALL REQUEST============================");

    // 5. task ID, task status 업데이트
    targetService.updateMonitoringAgentTaskStatusAndTaskId(nsId, mciId, targetId, TargetAgentTaskStatus.INSTALLING,
        String.valueOf(task.getId()));


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



    // 6) 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), nsId, mciId, targetId,
        SemaphoreInstallMethod.UNINSTALL, Agent.TELEGRAF);
  }




  @Transactional
  public List<ResultDTO> restart(String nsId, String mciId, String targetId) {

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
