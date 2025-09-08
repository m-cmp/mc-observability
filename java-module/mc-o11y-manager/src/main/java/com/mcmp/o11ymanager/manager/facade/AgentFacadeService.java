package com.mcmp.o11ymanager.manager.facade;

import static com.mcmp.o11ymanager.manager.service.domain.SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS;

import com.mcmp.o11ymanager.manager.dto.host.ConfigDTO;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;
import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.global.annotation.Base64Decode;
import com.mcmp.o11ymanager.manager.port.TumblebugPort;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class AgentFacadeService {

    private static final Lock semaphoreInstallTemplateCurrentCountLock = new ReentrantLock();

    private int semaphoreInstallTemplateCurrentCount = 0;
    private int semaphoreConfigUpdateTemplateCurrentCount = 0;

    private final TumblebugPort tumblebugPort;

    private final FluentBitFacadeService fluentBitFacadeService;
    private final TelegrafFacadeService telegrafFacadeService;
    private final ConcurrentHashMap<String, ReentrantLock> repositoryLocks =
            new ConcurrentHashMap<>();
    private final TumblebugService tumblebugService;

    private ReentrantLock getAgentLock(String nsId, String mciId, String vmId) {
        String lockKey = nsId + "-" + mciId + "-" + vmId;
        return repositoryLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
    }

    private AccessInfoDTO getAccessInfo(String nsId, String mciId, String vmId) {
        TumblebugMCI.Vm vm = tumblebugPort.getVM(nsId, mciId, vmId);
        TumblebugSshKey sshKey = tumblebugPort.getSshKey(nsId, vm.getSshKeyId());

        if (sshKey == null) {
            log.warn("🔴 SSH private key not found");
            throw new RuntimeException("SSH private key not found");
        } else {
            log.info(
                    "🔑 key name={}, id={}, privateKey={}",
                    sshKey.getName(),
                    sshKey.getId(),
                    sshKey.getPrivateKey());
        }

        return AccessInfoDTO.builder()
                .ip(vm.getPublicIP())
                .port(Integer.parseInt(vm.getSshPort()))
                .user(vm.getVmUserName())
                .sshKey(sshKey.getPrivateKey())
                .build();
    }

    public AgentServiceStatus getAgentServiceStatus(
            String nsId, String mciId, String vmId, Agent agent) {
        boolean isActive = tumblebugService.isServiceActive(nsId, mciId, vmId, agent);
        return isActive ? AgentServiceStatus.ACTIVE : AgentServiceStatus.INACTIVE;
    }

    public List<ResultDTO> install(String nsId, String mciId, String vmId) {

        log.info(
                "===================================start Agent Install - vmId: {}===========================================",
                vmId);

        List<ResultDTO> results = new ArrayList<>();
        ReentrantLock agentLock = getAgentLock(nsId, mciId, vmId);

        try {
            AccessInfoDTO accessInfo = getAccessInfo(nsId, mciId, vmId);

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

            telegrafFacadeService.install(nsId, mciId, vmId, accessInfo, templateCount);

            // 2-2 ) FluentBit 설치
            fluentBitFacadeService.install(nsId, mciId, vmId, accessInfo, templateCount);

            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.SUCCESS)
                            .build());

        } catch (Exception e) {
            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.ERROR)
                            .errorMessage(e.getMessage())
                            .build());
        } finally {
            if (agentLock.isLocked()) {
                agentLock.unlock();
            }
        }

        return results;
    }

    @Transactional
    public List<ResultDTO> update(String nsId, String mciId, String vmId) {
        List<ResultDTO> results = new ArrayList<>();
        ReentrantLock agentLock = getAgentLock(nsId, mciId, vmId);

        try {
            AccessInfoDTO accessInfo = getAccessInfo(nsId, mciId, vmId);

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
            telegrafFacadeService.update(nsId, mciId, vmId, accessInfo, templateCount);

            // 2-1 ) FluentBit 업데이트
            fluentBitFacadeService.update(nsId, mciId, vmId, accessInfo, templateCount);

            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.SUCCESS)
                            .build());
        } catch (Exception e) {
            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.ERROR)
                            .errorMessage(e.getMessage())
                            .build());
        } finally {
            if (agentLock.isLocked()) {
                agentLock.unlock();
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
    public List<ResultDTO> uninstall(String nsId, String mciId, String vmId) {

        List<ResultDTO> results = new ArrayList<>();

        ReentrantLock agentLock = getAgentLock(nsId, mciId, vmId);
        AccessInfoDTO accessInfo = getAccessInfo(nsId, mciId, vmId);

        try {
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
            telegrafFacadeService.uninstall(nsId, mciId, vmId, accessInfo, templateCount);

            // 4-1 ) FluentBit 제거
            fluentBitFacadeService.uninstall(nsId, mciId, vmId, accessInfo, templateCount);

            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.SUCCESS)
                            .build());
        } catch (Exception e) {
            results.add(
                    ResultDTO.builder()
                            .nsId(nsId)
                            .mciId(mciId)
                            .vmId(vmId)
                            .status(ResponseStatus.ERROR)
                            .errorMessage(e.getMessage())
                            .build());

        } finally {
            if (agentLock.isLocked()) {
                agentLock.unlock();
            }
        }

        return results;
    }

    //  @Transactional
    //  @Base64Decode(ConfigDTO.class)
    //  public List<ResultDTO> updateTelegrafConfig(ConfigDTO configDTO) {
    //
    //
    //    String vmId = configDTO.getVmId();
    //    String nsId = configDTO.getNsId();
    //    String mciId = configDTO.getMciId();
    //
    //    List<ResultDTO> results = new ArrayList<>();
    //    // TODO : telegraf만 lock 거는 메소드 추가 필요한지
    //      ReentrantLock monitoringLock = getAgentLock(nsId, mciId, vmId);
    //
    //      try {
    //        monitoringLock.lock();
    //
    //        // 2) 호스트 상태 확인
    //        vmService.isIdleMonitoringAgent(nsId, mciId, vmId);
    //        // 3) 에이전트 상태 확인
    //        vmService.isMonitoringAgentInstalled(nsId, mciId, vmId);
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
    //        telegrafConfigFacadeService.updateTelegrafConfig(vm, configDTO.getContent(),
    //            configDTO.getPath());
    //
    //        // 7) Semaphore 수정 요청
    //        Task task;
    //        HostConnectionDTO hostConnectionInfo = vmService.getHostConnectionInfo(hostId);
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
    //        vmService.updateMonitoringAgentTaskStatus(hostId,
    // HostAgentTaskStatus.UPDATING_CONFIG);
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
    //        vmService.isIdleLogAgent(hostId);
    //        // 3) 에이전트 상태 확인
    //        vmService.isLogAgentInstalled(hostId);
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
    //        vmService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.PREPARING);
    //
    //        // 6) 파일 수정
    //        fluentBitConfigFacadeService.updateFluentBitConfig(hostId, configDTO.getContent(),
    //            configDTO.getPath());
    //
    //        // 7) Semaphore 수정 요청
    //        Task task;
    //        HostConnectionDTO hostConnectionInfo = vmService.getHostConnectionInfo(hostId);
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
    //        vmService.updateLogAgentTaskStatus(hostId, HostAgentTaskStatus.UPDATING_CONFIG);
    //
    //
    //        // 10) 스케줄러 등록
    //        schedulerFacadeService.scheduleTaskStatusCheck(requestInfo.getRequestId(), taskId,
    // hostId,
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
