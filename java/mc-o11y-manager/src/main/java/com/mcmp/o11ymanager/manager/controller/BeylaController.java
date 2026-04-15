package com.mcmp.o11ymanager.manager.controller;

import static com.mcmp.o11ymanager.manager.service.domain.SemaphoreDomainService.SEMAPHORE_MAX_PARALLEL_TASKS;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugMCI;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugSshKey;
import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.facade.AgentFacadeService;
import com.mcmp.o11ymanager.manager.facade.BeylaFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.port.TumblebugPort;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator.BeylaSystemCheckResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring")
@Tag(
        name = "[Manager] Beyla Agent Management",
        description = "Beyla APM/Trace Agent management APIs")
public class BeylaController {

    private static final Lock semaphoreInstallTemplateCurrentCountLock = new ReentrantLock();
    private int semaphoreInstallTemplateCurrentCount = 0;

    private final BeylaFacadeService beylaFacadeService;
    private final AgentFacadeService agentFacadeService;
    private final TumblebugPort tumblebugPort;
    private final BeylaSystemRequirementValidator beylaSystemRequirementValidator;

    @PostMapping("/{nsId}/{mciId}/vm/{vmId}/beyla/install")
    @Operation(
            summary = "Install Beyla Agent",
            operationId = "InstallBeylaAgent",
            description = "Install Beyla APM/Trace agent on the target VM")
    public ResBody<Void> install(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "MCI ID", example = "mci-1") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "vm-1") @PathVariable String vmId)
            throws Exception {

        AccessInfoDTO accessInfo = getAccessInfo(nsId, mciId, vmId);
        int templateCount = getTemplateCount();

        beylaFacadeService.install(nsId, mciId, vmId, accessInfo, templateCount);

        return new ResBody<>();
    }

    @PutMapping("/{nsId}/{mciId}/vm/{vmId}/beyla/update")
    @Operation(
            summary = "Update Beyla Agent",
            operationId = "UpdateBeylaAgent",
            description = "Update Beyla APM/Trace agent on the target VM")
    public ResBody<Void> update(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "MCI ID", example = "mci-1") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "vm-1") @PathVariable String vmId)
            throws Exception {

        AccessInfoDTO accessInfo = getAccessInfo(nsId, mciId, vmId);
        int templateCount = getTemplateCount();

        beylaFacadeService.update(nsId, mciId, vmId, accessInfo, templateCount);

        return new ResBody<>();
    }

    @DeleteMapping("/{nsId}/{mciId}/vm/{vmId}/beyla/uninstall")
    @Operation(
            summary = "Uninstall Beyla Agent",
            operationId = "UninstallBeylaAgent",
            description = "Uninstall Beyla APM/Trace agent from the target VM")
    public ResBody<Void> uninstall(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "MCI ID", example = "mci-1") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "vm-1") @PathVariable String vmId) {

        AccessInfoDTO accessInfo = getAccessInfo(nsId, mciId, vmId);
        int templateCount = getTemplateCount();

        beylaFacadeService.uninstall(nsId, mciId, vmId, accessInfo, templateCount);

        return new ResBody<>();
    }

    @PostMapping("/{nsId}/{mciId}/vm/{vmId}/beyla/restart")
    @Operation(
            summary = "Restart Beyla Agent",
            operationId = "RestartBeylaAgent",
            description = "Restart Beyla APM/Trace agent on the target VM")
    public ResBody<List<ResultDTO>> restart(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "MCI ID", example = "mci-1") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "vm-1") @PathVariable String vmId) {

        List<ResultDTO> results = beylaFacadeService.restart(nsId, mciId, vmId);

        return new ResBody<>(results);
    }

    @GetMapping("/{nsId}/{mciId}/vm/{vmId}/beyla/status")
    @Operation(
            summary = "Get Beyla Agent Status",
            operationId = "GetBeylaAgentStatus",
            description = "Get Beyla APM/Trace agent status on the target VM")
    public ResBody<AgentStatus> getStatus(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "MCI ID", example = "mci-1") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "vm-1") @PathVariable String vmId) {

        AgentStatus status = agentFacadeService.getAgentStatus(nsId, mciId, vmId, Agent.BEYLA);

        return new ResBody<>(status);
    }

    @GetMapping("/{nsId}/{mciId}/vm/{vmId}/beyla/system-check")
    @Operation(
            summary = "Check Beyla System Requirements",
            operationId = "CheckBeylaSystemRequirements",
            description =
                    "Check if the target VM meets Beyla system requirements (kernel version, BTF support)")
    public ResBody<BeylaSystemCheckResult> checkSystemRequirements(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "MCI ID", example = "mci-1") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "vm-1") @PathVariable String vmId) {

        BeylaSystemCheckResult result = beylaSystemRequirementValidator.validate(nsId, mciId, vmId);

        return new ResBody<>(result);
    }

    private AccessInfoDTO getAccessInfo(String nsId, String mciId, String vmId) {
        TumblebugMCI.Vm vm = tumblebugPort.getVM(nsId, mciId, vmId);
        TumblebugSshKey sshKey = tumblebugPort.getSshKey(nsId, vm.getSshKeyId());

        if (sshKey == null) {
            log.warn("SSH private key not found");
            throw new RuntimeException("SSH private key not found");
        }

        return AccessInfoDTO.builder()
                .ip(vm.getPublicIP())
                .port(Integer.parseInt(vm.getSshPort()))
                .user(vm.getVmUserName())
                .sshKey(sshKey.getPrivateKey())
                .build();
    }

    // 확인 필요: 기존에는 `> SEMAPHORE_MAX_PARALLEL_TASKS` 조건이라 카운터가 11까지 올라가
    // `agent_install_11` 템플릿을 찾다가 NoSuchElementException이 발생할 수 있었음.
    // `>=`로 바꿔 1~10 범위로만 순환하도록 수정. 동일 패턴이 AgentFacadeService에도 남아 있음.
    private int getTemplateCount() {
        try {
            semaphoreInstallTemplateCurrentCountLock.lock();
            if (semaphoreInstallTemplateCurrentCount >= SEMAPHORE_MAX_PARALLEL_TASKS) {
                semaphoreInstallTemplateCurrentCount = 0;
            }
            semaphoreInstallTemplateCurrentCount++;
            return semaphoreInstallTemplateCurrentCount;
        } finally {
            semaphoreInstallTemplateCurrentCountLock.unlock();
        }
    }
}
