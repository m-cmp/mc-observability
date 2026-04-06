package com.mcmp.o11ymanager.manager.controller;

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
@Tag(name = "[Manager] Monitoring VM Management")
public class VMController {

    private final VMFacadeService vmFacadeService;

    @GetMapping("/{nsId}/{mciId}/vm/{vmId}")
    @Operation(summary = "GetVM", operationId = "GetVM", description = "Get target (VM)")
    public ResBody<VMDTO> getVM(
            @Parameter(description = "Namespace ID (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "MCI ID (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "VM ID (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId) {
        return new ResBody<>(vmFacadeService.getVM(nsId, mciId, vmId));
    }

    @PostMapping("/{nsId}/{mciId}/vm/{vmId}")
    @Operation(summary = "PostVM", operationId = "PostVM", description = "Create target (VM)")
    public ResBody<Void> postVM(
            @Parameter(description = "Namespace ID (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "MCI ID (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "VM ID (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId,
            @RequestBody @Valid VMRequestDTO dto) {
        vmFacadeService.postVM(nsId, mciId, vmId, dto);
        return new ResBody<>();
    }

    @PutMapping("/{nsId}/{mciId}/vm/{vmId}")
    @Operation(summary = "PutVM", operationId = "PutVM", description = "Update target")
    public ResBody<VMDTO> putVM(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId,
            @RequestBody VMRequestDTO dto) {
        return new ResBody<>(vmFacadeService.putVM(nsId, mciId, vmId, dto));
    }

    @DeleteMapping("/{nsId}/{mciId}/vm/{vmId}")
    @Operation(summary = "DeleteVM", operationId = "DeleteVM", description = "Delete target")
    public ResBody<Void> deleteVM(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId) {
        vmFacadeService.deleteVM(nsId, mciId, vmId);
        return new ResBody<>();
    }

    @GetMapping("/{nsId}/{mciId}/vm")
    @Operation(
            summary = "GetVMByNsMci",
            operationId = "GetVMByNsMci",
            description = "Retrieve target list by NS/MCI")
    public ResBody<List<VMDTO>> getVMByNsMci(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId) {
        return new ResBody<>(vmFacadeService.getVMsNsMci(nsId, mciId));
    }

    @GetMapping("/vm")
    @Operation(
            summary = "GetAllVMs",
            operationId = "GetAllVMs",
            description = "Retrieve all targets")
    public ResBody<List<VMDTO>> getAllVMs() {
        return new ResBody<>(vmFacadeService.getVMs());
    }
}
