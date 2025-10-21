package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.vm.VMDTO;
import com.mcmp.o11ymanager.manager.dto.vm.VMRequestDTO;
import com.mcmp.o11ymanager.manager.facade.VMFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
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
public class VMController {

    private final VMFacadeService vmFacadeService;

    @GetMapping("/{nsId}/{mciId}/vm/{vmId}")
    public ResBody<VMDTO> getVM(
            @PathVariable String nsId, @PathVariable String mciId, @PathVariable String vmId) {
        return new ResBody<>(vmFacadeService.getVM(nsId, mciId, vmId));
    }

    @PostMapping("/{nsId}/{mciId}/vm/{vmId}")
    public ResBody<Void> postVM(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @RequestBody @Valid VMRequestDTO dto) {
        vmFacadeService.postVM(nsId, mciId, vmId, dto);
        return new ResBody<>();
    }

    @PutMapping("/{nsId}/{mciId}/vm/{vmId}")
    public ResBody<VMDTO> putVM(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @RequestBody VMRequestDTO dto) {
        return new ResBody<>(vmFacadeService.putVM(nsId, mciId, vmId, dto));
    }

    @DeleteMapping("/{nsId}/{mciId}/vm/{vmId}")
    public ResBody<Void> deleteVM(
            @PathVariable String nsId, @PathVariable String mciId, @PathVariable String vmId) {
        vmFacadeService.deleteVM(nsId, mciId, vmId);
        return new ResBody<>();
    }

    @GetMapping("/{nsId}/{mciId}/vm")
    public ResBody<List<VMDTO>> getVMByNsMci(
            @PathVariable String nsId, @PathVariable String mciId) {
        return new ResBody<>(vmFacadeService.getVMsNsMci(nsId, mciId));
    }

    @GetMapping("/vm")
    public ResBody<List<VMDTO>> getAllVMs() {
        return new ResBody<>(vmFacadeService.getVMs());
    }
}
