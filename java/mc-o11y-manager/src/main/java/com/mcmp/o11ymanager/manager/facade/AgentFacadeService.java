package com.mcmp.o11ymanager.manager.facade;

import static com.mcmp.o11ymanager.manager.service.domain.SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS;

import com.mcmp.o11ymanager.manager.dto.host.ConfigDTO;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;
import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentServiceStatus;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.enums.ResponseStatus;
import com.mcmp.o11ymanager.manager.global.annotation.Base64Decode;
import com.mcmp.o11ymanager.manager.model.host.VMAgentTaskStatus;
import com.mcmp.o11ymanager.manager.port.TumblebugPort;
import com.mcmp.o11ymanager.manager.service.interfaces.TumblebugService;
import com.mcmp.o11ymanager.manager.service.interfaces.VMService;
import java.util.ArrayList;
import java.util.List;
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
    private final BeylaFacadeService beylaFacadeService;
    private final TumblebugService tumblebugService;
    private final VMService vmService;

    private AccessInfoDTO getAccessInfo(String nsId, String mciId, String vmId) {

        TumblebugMCI.Vm vm = tumblebugPort.getVM(nsId, mciId, vmId);
        TumblebugSshKey sshKey = tumblebugPort.getSshKey(nsId, vm.getSshKeyId());

        if (sshKey == null) {
            log.warn("SSH private key not found");
            throw new RuntimeException("SSH private key not found");
        } else {
            log.info("key name={}, id={}", sshKey.getName(), sshKey.getId());
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
                "=================================== Start Agent Installation - vmId: {} ===========================================",
                vmId);

        List<ResultDTO> results = new ArrayList<>();

        try {
            AccessInfoDTO accessInfo = getAccessInfo(nsId, mciId, vmId);

            // 1) Acquire lock
            int templateCount;
            try {
                semaphoreInstallTemplateCurrentCountLock.lock();
                templateCount = getSemaphoreInstallTemplateCurrentCount();
            } finally {
                semaphoreInstallTemplateCurrentCountLock.unlock();
            }

            // 2) Install agent
            // 2-1) Install Telegraf
            telegrafFacadeService.install(nsId, mciId, vmId, accessInfo, templateCount);

            // 2-2) Install FluentBit
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
        }

        return results;
    }

    @Transactional
    public List<ResultDTO> update(String nsId, String mciId, String vmId) {
        List<ResultDTO> results = new ArrayList<>();

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
        }

        return results;
    }

    // 확인 필요: 기존에는 `> SEMAPHORE_MAX_PARALLEL_TASKS` 조건이라 카운터가 11까지 올라가
    // `agent_install_11` 템플릿을 찾다가 NoSuchElementException이 발생할 수 있었음.
    // `>=`로 바꿔 1~10 범위로만 순환하도록 수정. Telegraf/FluentBit 공용 경로라 영향 범위 주의.
    private int getSemaphoreInstallTemplateCurrentCount() {
        if (semaphoreInstallTemplateCurrentCount >= SEMAPHORE_MAX_PARALLEL_TASKS) {
            semaphoreInstallTemplateCurrentCount = 0;
        }
        semaphoreInstallTemplateCurrentCount++;

        return semaphoreInstallTemplateCurrentCount;
    }

    // 확인 필요: 위 getSemaphoreInstallTemplateCurrentCount와 동일 이슈.
    // `>`를 `>=`로 바꿔 config update 템플릿 슬롯(1~10) 범위 밖을 참조하지 않도록 수정.
    private int getSemaphoreConfigUpdateTemplateCurrentCount() {
        if (semaphoreConfigUpdateTemplateCurrentCount >= SEMAPHORE_MAX_PARALLEL_TASKS) {
            semaphoreConfigUpdateTemplateCurrentCount = 0;
        }
        semaphoreConfigUpdateTemplateCurrentCount++;

        return semaphoreConfigUpdateTemplateCurrentCount;
    }

    public AgentStatus getAgentStatus(String nsId, String mciId, String vmId, Agent agent) {
        VMAgentTaskStatus taskStatus;

        if (agent == Agent.TELEGRAF) {
            taskStatus = vmService.getMonitoringAgentTaskStatus(nsId, mciId, vmId);
        } else if (agent == Agent.FLUENT_BIT) {
            taskStatus = vmService.getLogAgentTaskStatus(nsId, mciId, vmId);
        } else if (agent == Agent.BEYLA) {
            taskStatus = vmService.getTraceAgentTaskStatus(nsId, mciId, vmId);
        } else {
            throw new IllegalArgumentException("Unknown agent type: " + agent);
        }

        if (taskStatus == VMAgentTaskStatus.INSTALLING) {
            return AgentStatus.INSTALLING;
        }
        if (taskStatus == VMAgentTaskStatus.FAILED) {
            return AgentStatus.FAILED;
        }

        AgentServiceStatus serviceStatus = getAgentServiceStatus(nsId, mciId, vmId, agent);

        if (serviceStatus == AgentServiceStatus.ACTIVE) {
            return AgentStatus.SUCCESS;
        } else {
            return AgentStatus.SERVICE_INACTIVE;
        }
    }

    @Transactional
    @Base64Decode(ConfigDTO.class)
    public List<ResultDTO> uninstall(String nsId, String mciId, String vmId) {

        List<ResultDTO> results = new ArrayList<>();

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
        }

        return results;
    }
}
