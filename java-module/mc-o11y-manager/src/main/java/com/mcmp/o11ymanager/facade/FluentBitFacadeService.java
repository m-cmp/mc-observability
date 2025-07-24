package com.mcmp.o11ymanager.facade;

import com.mcmp.o11ymanager.dto.target.ResultDTO;
import com.mcmp.o11ymanager.entity.TargetEntity;
import com.mcmp.o11ymanager.enums.Agent;
import com.mcmp.o11ymanager.enums.AgentAction;
import com.mcmp.o11ymanager.enums.ResponseStatus;
import com.mcmp.o11ymanager.enums.SemaphoreInstallMethod;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
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
@RequiredArgsConstructor
@Service
public class FluentBitFacadeService {

  private final FileFacadeService fileFacadeService;
  private final TargetService targetService;
  private static final Lock agentTaskStatusLock = new ReentrantLock();
  private final RequestInfo requestInfo;
  private final SemaphoreDomainService semaphoreDomainService;
  private final SchedulerFacadeService schedulerFacadeService;
  private final FluentBitConfigFacadeService fluentBitConfigFacadeService;

  public void install(String nsId, String mciId, String targetId,
      @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    targetService.isIdleLogAgent(nsId, mciId, targetId);

    // 2. host 상태 업데이트
    targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.INSTALLING);

    String configContent = fluentBitConfigFacadeService.initFluentbitConfig(nsId, mciId, targetId);

    // 4. 전송(semaphore) - 설치 요청
    Task task = semaphoreDomainService.install(nsId, mciId, targetId, SemaphoreInstallMethod.INSTALL,
        configContent, Agent.FLUENT_BIT,
        templateCount);

    // 5. task ID, task status 업데이트
    targetService.updateLogAgentTaskStatusAndTaskId(nsId, mciId, targetId, TargetAgentTaskStatus.INSTALLING,
        String.valueOf(task.getId()));

    // 7. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), nsId, mciId, targetId,
        SemaphoreInstallMethod.INSTALL, Agent.FLUENT_BIT);
  }

  public void update(String nsId, String mciId, String targetId,
                     @NotBlank int templateCount) throws Exception {

    // 1. host IDLE 상태 확인
    targetService.isIdleLogAgent(nsId, mciId, targetId);

    // 2. host 상태 업데이트
    targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.UPDATING);

    // 3. 전송(semaphore) - 업데이트 요청
    Task task = semaphoreDomainService.install(nsId, mciId, targetId, SemaphoreInstallMethod.UPDATE,
            null, Agent.FLUENT_BIT,
            templateCount);

    // 4. task ID, task status 업데이트
    targetService.updateLogAgentTaskStatusAndTaskId(nsId, mciId, targetId, TargetAgentTaskStatus.UPDATING,
            String.valueOf(task.getId()));


    // 6. 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), nsId, mciId, targetId,
            SemaphoreInstallMethod.UPDATE, Agent.FLUENT_BIT);
  }

  public void uninstall(String nsId, String mciId, String targetId, int templateCount, String requestUserId) throws Exception {

    // 1) 상태 확인
    targetService.isIdleLogAgent(nsId, mciId, targetId);

    // 2) 상태 변경
    targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.PREPARING);

    // 3. 전송(semaphore) - 삭제 요청
    Task task = semaphoreDomainService.install(nsId, mciId, targetId, SemaphoreInstallMethod.UNINSTALL,
        null, Agent.FLUENT_BIT,
        templateCount);

    // 5. task ID, task status 업데이트
    targetService.updateLogAgentTaskStatusAndTaskId(nsId, mciId, targetId, TargetAgentTaskStatus.UNINSTALLING,
        String.valueOf(task.getId()));

    // 6) 스케줄러 등록
    schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), task.getId(), nsId, mciId, targetId,
        SemaphoreInstallMethod.UNINSTALL, Agent.FLUENT_BIT);

  }

  @Transactional
  public List<ResultDTO> enable(String nsId, String mciId, String targetId, String requestUserId) {
    List<ResultDTO> results = new ArrayList<>();

    try {
      agentTaskStatusLock.lock();

      TargetEntity target;
      target = targetService.get(nsId, mciId, targetId).toEntity();

      // 1. 싫행 상태 확인
      targetService.isIdleLogAgent(nsId, mciId, targetId);

      // 2. ENABLING 상태로 변경
      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.ENABLING);

      // TODO : Use Tumblebug CMD - 3, 활성화 실행
//      SshConnection connection = sshService.getConnection(
//              target.getIp(), target.getPort(), target.getUser(), target.getPassword());
//
//      sshService.enableFluentBit(connection, target.getIp(), target.getPort(),
//              target.getUser(), target.getPassword());

      // 4. 모든 작업 완료 후 상태 IDLE(실행 요청 enabling에서) 업데이트
      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

      agentTaskStatusLock.unlock();

      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());
    } catch (Exception e) {

      agentTaskStatusLock.unlock();

      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);


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

      // 1. 싫행 상태 확인
      targetService.isIdleLogAgent(nsId, mciId, targetId);

      // 2. DISABLING 상태로 변경
      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.DISABLING);

      // TODO : Use Tumblebug CMD - 3. 비활성화 실행
//      SshConnection connection = sshService.getConnection(
//              target.getIp(), target.getPort(), target.getUser(), target.getPassword());
//
//      sshService.disableFluentBit(connection, target.getIp(), target.getPort(),
//              target.getUser(), target.getPassword());

      // 4. 작업 완료 후 idle 변경
      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);


      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());

      agentTaskStatusLock.unlock();
    } catch (Exception e) {

      agentTaskStatusLock.unlock();

      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

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

      // 1. 싫행 상태 확인
      targetService.isIdleLogAgent(nsId, mciId, targetId);

      // 2. RESTARTING 상태로 변경
      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.RESTARTING);

      // TODO : Use Tumblebug CMD - 3. restart 실행
//      SshConnection connection = sshService.getConnection(
//              target.getIp(), target.getPort(), target.getUser(), target.getPassword());
//
//      sshService.restartFluentBit(connection, target.getIp(), target.getPort(),
//              target.getUser(), target.getPassword());

      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);


      results.add(ResultDTO.builder()
              .nsId(nsId)
              .mciId(mciId)
              .targetId(targetId)
              .status(ResponseStatus.SUCCESS)
              .build());
      agentTaskStatusLock.unlock();
    } catch (Exception e) {
      agentTaskStatusLock.unlock();

      targetService.updateLogAgentTaskStatus(nsId, mciId, targetId, TargetAgentTaskStatus.IDLE);

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
