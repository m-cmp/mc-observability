package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.facade.AgentFacadeService;
import com.mcmp.o11ymanager.manager.facade.BeylaFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.service.SemaphoreInstallTemplateCounter;
import com.mcmp.o11ymanager.manager.service.VmAccessInfoResolver;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator;
import com.mcmp.o11ymanager.manager.service.domain.BeylaSystemRequirementValidator.BeylaSystemCheckResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Linux VM 대상 Beyla(eBPF) trace agent 관리 API.
 *
 * <p>Windows VM 대상은 {@link OtelJavaController}(URL prefix {@code /windows-trace-agent})를 사용. 두
 * controller가 동일한 trace agent 상태 컬럼을 공유하므로 한 VM에 대해 동시 호출은 BeylaFacadeService /
 * OtelJavaFacadeService 내부의 lock으로 직렬화된다.
 *
 * <p>Windows VM에 본 endpoint를 호출하면 명시적으로 throw하여 caller가 올바른 endpoint로 안내한다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring")
@Tag(
        name = "[Manager] Beyla Agent Management",
        description = "Linux VM 대상 Beyla(eBPF) APM/Trace Agent management APIs")
public class BeylaController {

    private final BeylaFacadeService beylaFacadeService;
    private final AgentFacadeService agentFacadeService;
    private final BeylaSystemRequirementValidator beylaSystemRequirementValidator;
    private final VmAccessInfoResolver vmAccessInfoResolver;
    private final SemaphoreInstallTemplateCounter templateCounter;

    @PostMapping("/{nsId}/{infraId}/node/{nodeId}/beyla/install")
    @Operation(
            summary = "Install Beyla trace agent (Linux only)",
            operationId = "InstallTraceAgent",
            description =
                    "Linux VM에 Beyla eBPF trace agent를 설치한다. Windows VM에 호출하면 throw하므로,"
                            + " Windows에는 /windows-trace-agent/install을 사용하라.")
    public ResBody<Void> install(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "Infra ID", example = "infra-1") @PathVariable String infraId,
            @Parameter(description = "Node ID", example = "node-1") @PathVariable String nodeId)
            throws Exception {

        ensureLinux(nsId, infraId, nodeId);
        AccessInfoDTO accessInfo = vmAccessInfoResolver.resolve(nsId, infraId, nodeId);
        int templateCount = templateCounter.next();
        beylaFacadeService.install(nsId, infraId, nodeId, accessInfo, templateCount);
        return new ResBody<>();
    }

    @PutMapping("/{nsId}/{infraId}/node/{nodeId}/beyla/update")
    @Operation(summary = "Update Beyla trace agent (Linux only)", operationId = "UpdateTraceAgent")
    public ResBody<Void> update(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "Infra ID", example = "infra-1") @PathVariable String infraId,
            @Parameter(description = "Node ID", example = "node-1") @PathVariable String nodeId)
            throws Exception {

        ensureLinux(nsId, infraId, nodeId);
        AccessInfoDTO accessInfo = vmAccessInfoResolver.resolve(nsId, infraId, nodeId);
        int templateCount = templateCounter.next();
        beylaFacadeService.update(nsId, infraId, nodeId, accessInfo, templateCount);
        return new ResBody<>();
    }

    @DeleteMapping("/{nsId}/{infraId}/node/{nodeId}/beyla/uninstall")
    @Operation(
            summary = "Uninstall Beyla trace agent (Linux only)",
            operationId = "UninstallTraceAgent")
    public ResBody<Void> uninstall(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "Infra ID", example = "infra-1") @PathVariable String infraId,
            @Parameter(description = "Node ID", example = "node-1") @PathVariable String nodeId) {

        ensureLinux(nsId, infraId, nodeId);
        AccessInfoDTO accessInfo = vmAccessInfoResolver.resolve(nsId, infraId, nodeId);
        int templateCount = templateCounter.next();
        beylaFacadeService.uninstall(nsId, infraId, nodeId, accessInfo, templateCount);
        return new ResBody<>();
    }

    @PostMapping("/{nsId}/{infraId}/node/{nodeId}/beyla/restart")
    @Operation(
            summary = "Restart Beyla trace agent (Linux only)",
            operationId = "RestartTraceAgent")
    public ResBody<List<ResultDTO>> restart(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "Infra ID", example = "infra-1") @PathVariable String infraId,
            @Parameter(description = "Node ID", example = "node-1") @PathVariable String nodeId) {

        ensureLinux(nsId, infraId, nodeId);
        List<ResultDTO> results = beylaFacadeService.restart(nsId, infraId, nodeId);
        return new ResBody<>(results);
    }

    @GetMapping("/{nsId}/{infraId}/node/{nodeId}/beyla/status")
    @Operation(
            summary = "Get Beyla Agent Status",
            operationId = "GetBeylaAgentStatus",
            description = "Get Beyla APM/Trace agent status on the target VM")
    public ResBody<AgentStatus> getStatus(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "Infra ID", example = "infra-1") @PathVariable String infraId,
            @Parameter(description = "Node ID", example = "node-1") @PathVariable String nodeId) {

        AgentStatus status = agentFacadeService.getAgentStatus(nsId, infraId, nodeId, Agent.BEYLA);
        return new ResBody<>(status);
    }

    @GetMapping("/{nsId}/{infraId}/node/{nodeId}/beyla/system-check")
    @Operation(
            summary = "Check Beyla System Requirements (Linux only)",
            operationId = "CheckBeylaSystemRequirements",
            description =
                    "Check if the target VM meets Beyla system requirements (kernel version, BTF support)")
    public ResBody<BeylaSystemCheckResult> checkSystemRequirements(
            @Parameter(description = "Namespace ID", example = "ns-1") @PathVariable String nsId,
            @Parameter(description = "Infra ID", example = "infra-1") @PathVariable String infraId,
            @Parameter(description = "Node ID", example = "node-1") @PathVariable String nodeId) {

        BeylaSystemCheckResult result =
                beylaSystemRequirementValidator.validate(nsId, infraId, nodeId);
        return new ResBody<>(result);
    }

    /** Linux 전용 엔드포인트. Windows node면 400 BAD_REQUEST로 caller에 올바른 endpoint 안내. */
    private void ensureLinux(String nsId, String infraId, String nodeId) {
        if (vmAccessInfoResolver.isWindowsNode(nsId, infraId, nodeId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This endpoint is for Linux nodes only. For Windows nodes, use"
                            + " /windows-trace-agent/...");
        }
    }
}
