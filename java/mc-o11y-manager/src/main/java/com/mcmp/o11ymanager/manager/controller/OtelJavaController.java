package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.vm.AccessInfoDTO;
import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.enums.Agent;
import com.mcmp.o11ymanager.manager.enums.AgentStatus;
import com.mcmp.o11ymanager.manager.facade.AgentFacadeService;
import com.mcmp.o11ymanager.manager.facade.OtelJavaFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.service.SemaphoreInstallTemplateCounter;
import com.mcmp.o11ymanager.manager.service.VmAccessInfoResolver;
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
 * Windows VM 대상 OpenTelemetry Java Auto-Instrumentation agent 관리 API.
 *
 * <p>{@link BeylaController}(Linux 전용)와 별도 endpoint prefix({@code /windows-trace-agent})를 사용해 URL만
 * 봐도 어떤 agent가 설치되는지 명확하게 한다. 두 controller 모두 동일한 trace agent 상태 컬럼 ({@code
 * vmTraceAgentTaskStatus}/{@code vmTraceAgentTaskId})을 공유하므로 한 VM에 대해 동시 호출이 들어오면 {@code
 * BeylaFacadeService}/{@code OtelJavaFacadeService} 내부의 {@code agentTaskStatusLock}으로 직렬화된다.
 *
 * <p>Windows VM이 아닌 곳에 호출하면 명시적으로 throw하여 잘못된 endpoint 사용을 빠르게 알린다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring")
@Tag(
        name = "[Manager] Windows Trace Agent Management",
        description =
                "Windows VM 대상 OpenTelemetry Java Auto-Instrumentation agent install/update/uninstall/status APIs")
public class OtelJavaController {

    private final OtelJavaFacadeService otelJavaFacadeService;
    private final AgentFacadeService agentFacadeService;
    private final VmAccessInfoResolver vmAccessInfoResolver;
    private final SemaphoreInstallTemplateCounter templateCounter;

    @PostMapping("/{nsId}/{mciId}/vm/{vmId}/windows-trace-agent/install")
    @Operation(
            summary = "Install Windows OTel Java Auto-Instrumentation agent",
            operationId = "InstallWindowsTraceAgent",
            description =
                    "Windows VM에 OpenTelemetry Java Auto-Instrumentation jar 및 환경변수를 설치한다."
                            + " Linux VM에 호출하면 throw하므로, Linux에는 /beyla/install을 사용하라.")
    public ResBody<Void> install(
            @Parameter(description = "Namespace ID", example = "testns01") @PathVariable
                    String nsId,
            @Parameter(description = "MCI ID", example = "win2019-os01") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "win-1") @PathVariable String vmId)
            throws Exception {

        ensureWindows(nsId, mciId, vmId);
        AccessInfoDTO accessInfo = vmAccessInfoResolver.resolve(nsId, mciId, vmId);
        int templateCount = templateCounter.next();
        otelJavaFacadeService.install(nsId, mciId, vmId, accessInfo, templateCount);
        return new ResBody<>();
    }

    @PutMapping("/{nsId}/{mciId}/vm/{vmId}/windows-trace-agent/update")
    @Operation(
            summary = "Update Windows OTel Java agent (jar 재다운로드)",
            operationId = "UpdateWindowsTraceAgent")
    public ResBody<Void> update(
            @Parameter(description = "Namespace ID", example = "testns01") @PathVariable
                    String nsId,
            @Parameter(description = "MCI ID", example = "win2019-os01") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "win-1") @PathVariable String vmId)
            throws Exception {

        ensureWindows(nsId, mciId, vmId);
        AccessInfoDTO accessInfo = vmAccessInfoResolver.resolve(nsId, mciId, vmId);
        int templateCount = templateCounter.next();
        otelJavaFacadeService.update(nsId, mciId, vmId, accessInfo, templateCount);
        return new ResBody<>();
    }

    @DeleteMapping("/{nsId}/{mciId}/vm/{vmId}/windows-trace-agent/uninstall")
    @Operation(
            summary = "Uninstall Windows OTel Java agent",
            operationId = "UninstallWindowsTraceAgent",
            description = "환경변수 제거 + jar 삭제 + 사이트 폴더 정리. 호스트의 Java 앱은 재시작해야 trace 송신이 완전히 중지된다.")
    public ResBody<Void> uninstall(
            @Parameter(description = "Namespace ID", example = "testns01") @PathVariable
                    String nsId,
            @Parameter(description = "MCI ID", example = "win2019-os01") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "win-1") @PathVariable String vmId) {

        ensureWindows(nsId, mciId, vmId);
        AccessInfoDTO accessInfo = vmAccessInfoResolver.resolve(nsId, mciId, vmId);
        int templateCount = templateCounter.next();
        otelJavaFacadeService.uninstall(nsId, mciId, vmId, accessInfo, templateCount);
        return new ResBody<>();
    }

    /**
     * Windows OTel Java agent는 호스트 단위 환경변수 주입 방식이라 service 단위 restart 의미 없음. {@link
     * OtelJavaFacadeService#restart} 내부에서 미지원 throw하지만 caller에 명확하게 ERROR 결과로 응답한다.
     */
    @PostMapping("/{nsId}/{mciId}/vm/{vmId}/windows-trace-agent/restart")
    @Operation(
            summary = "Restart Windows OTel Java agent (미지원)",
            operationId = "RestartWindowsTraceAgent",
            description = "Windows에선 호스트 환경변수 주입 방식이라 별도 restart 의미 없음. 항상 ERROR 응답.")
    public ResBody<List<ResultDTO>> restart(
            @Parameter(description = "Namespace ID", example = "testns01") @PathVariable
                    String nsId,
            @Parameter(description = "MCI ID", example = "win2019-os01") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "win-1") @PathVariable String vmId) {

        ensureWindows(nsId, mciId, vmId);
        List<ResultDTO> results = otelJavaFacadeService.restart(nsId, mciId, vmId);
        return new ResBody<>(results);
    }

    @GetMapping("/{nsId}/{mciId}/vm/{vmId}/windows-trace-agent/status")
    @Operation(
            summary = "Get Windows OTel Java agent status",
            operationId = "GetWindowsTraceAgentStatus")
    public ResBody<AgentStatus> getStatus(
            @Parameter(description = "Namespace ID", example = "testns01") @PathVariable
                    String nsId,
            @Parameter(description = "MCI ID", example = "win2019-os01") @PathVariable String mciId,
            @Parameter(description = "VM ID", example = "win-1") @PathVariable String vmId) {

        AgentStatus status =
                agentFacadeService.getAgentStatus(nsId, mciId, vmId, Agent.OTEL_JAVA_AGENT);
        return new ResBody<>(status);
    }

    /** 잘못된 endpoint 사용 가드. Windows node가 아니면 명시적으로 throw해서 caller가 /beyla/...로 가도록 안내. */
    private void ensureWindows(String nsId, String mciId, String vmId) {
        if (!vmAccessInfoResolver.isWindowsNode(nsId, mciId, vmId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This endpoint is for Windows nodes only. For Linux nodes, use /beyla/...");
        }
    }
}
