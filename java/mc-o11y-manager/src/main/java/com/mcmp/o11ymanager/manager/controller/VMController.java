package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.vm.ResultDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.facade.VMFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring")
@Tag(name = "[Manager] Monitoring Node Management")
public class VMController {

    private final VMFacadeService vmFacadeService;

    @GetMapping("/{nsId}/{infraId}/node/{nodeId}")
    @Operation(summary = "GetVM", operationId = "GetVM", description = "Get target (VM)")
    public ResBody<VMDTO> getVM(
            @Parameter(description = "Namespace ID (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "Infra ID (e.g., infra-1)", example = "infra-1") @PathVariable
                    String infraId,
            @Parameter(description = "Node ID (e.g., node-1)", example = "node-1") @PathVariable
                    String nodeId) {
        return new ResBody<>(vmFacadeService.getVM(nsId, infraId, nodeId));
    }

    @PostMapping("/{nsId}/{infraId}/node/{nodeId}")
    @Operation(summary = "PostVM", operationId = "PostVM", description = "Create target (VM)")
    public ResBody<Void> postVM(
            @Parameter(description = "Namespace ID (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "Infra ID (e.g., infra-1)", example = "infra-1") @PathVariable
                    String infraId,
            @Parameter(description = "Node ID (e.g., node-1)", example = "node-1") @PathVariable
                    String nodeId,
            @RequestBody @Valid VMRequestDTO dto) {
        vmFacadeService.postVM(nsId, infraId, nodeId, dto);
        return new ResBody<>();
    }

    @PostMapping("/{nsId}/{infraId}/node/{nodeId}/monitoring-agent")
    @Operation(
            summary = "InstallMonitoringAgent",
            description = "Install only the monitoring agent (telegraf) on the node")
    public ResBody<List<ResultDTO>> installMonitoringAgent(
            @PathVariable String nsId, @PathVariable String infraId, @PathVariable String nodeId) {
        return new ResBody<>(vmFacadeService.installMonitoringAgent(nsId, infraId, nodeId));
    }

    @DeleteMapping("/{nsId}/{infraId}/node/{nodeId}/monitoring-agent")
    @Operation(
            summary = "UninstallMonitoringAgent",
            description = "Uninstall only the monitoring agent (telegraf) from the node")
    public ResBody<List<ResultDTO>> uninstallMonitoringAgent(
            @PathVariable String nsId, @PathVariable String infraId, @PathVariable String nodeId) {
        return new ResBody<>(vmFacadeService.uninstallMonitoringAgent(nsId, infraId, nodeId));
    }

    @PostMapping("/{nsId}/{infraId}/node/{nodeId}/log-agent")
    @Operation(
            summary = "InstallLogAgent",
            description = "Install only the log agent (fluent-bit) on the node")
    public ResBody<List<ResultDTO>> installLogAgent(
            @PathVariable String nsId, @PathVariable String infraId, @PathVariable String nodeId) {
        return new ResBody<>(vmFacadeService.installLogAgent(nsId, infraId, nodeId));
    }

    @DeleteMapping("/{nsId}/{infraId}/node/{nodeId}/log-agent")
    @Operation(
            summary = "UninstallLogAgent",
            description = "Uninstall only the log agent (fluent-bit) from the node")
    public ResBody<List<ResultDTO>> uninstallLogAgent(
            @PathVariable String nsId, @PathVariable String infraId, @PathVariable String nodeId) {
        return new ResBody<>(vmFacadeService.uninstallLogAgent(nsId, infraId, nodeId));
    }

    @PutMapping("/{nsId}/{infraId}/node/{nodeId}")
    @Operation(summary = "PutVM", operationId = "PutVM", description = "Update target")
    public ResBody<VMDTO> putVM(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "infraId (e.g., infra-1)", example = "infra-1") @PathVariable
                    String infraId,
            @Parameter(description = "nodeId (e.g., node-1)", example = "node-1") @PathVariable
                    String nodeId,
            @RequestBody VMRequestDTO dto) {
        return new ResBody<>(vmFacadeService.putVM(nsId, infraId, nodeId, dto));
    }

    @DeleteMapping("/{nsId}/{infraId}/node/{nodeId}")
    @Operation(summary = "DeleteVM", operationId = "DeleteVM", description = "Delete target")
    public ResBody<Void> deleteVM(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "infraId (e.g., infra-1)", example = "infra-1") @PathVariable
                    String infraId,
            @Parameter(description = "nodeId (e.g., node-1)", example = "node-1") @PathVariable
                    String nodeId) {
        vmFacadeService.deleteVM(nsId, infraId, nodeId);
        return new ResBody<>();
    }

    @GetMapping("/{nsId}/{infraId}/node")
    @Operation(
            summary = "GetVMByNsMci",
            operationId = "GetVMByNsMci",
            description = "Retrieve target list by NS/MCI")
    public ResBody<List<VMDTO>> getVMByNsMci(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "infraId (e.g., infra-1)", example = "infra-1") @PathVariable
                    String infraId) {
        return new ResBody<>(vmFacadeService.getVMsNsMci(nsId, infraId));
    }

    @GetMapping("/node")
    @Operation(
            summary = "GetAllVMs",
            operationId = "GetAllVMs",
            description = "Retrieve all targets")
    public ResBody<List<VMDTO>> getAllVMs() {
        return new ResBody<>(vmFacadeService.getVMs());
    }
}
