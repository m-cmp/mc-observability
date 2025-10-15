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
        ReentrantLock agentLock = getAgentLock(nsId, mciId, vmId);

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
            agentLock.lock();
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
}
