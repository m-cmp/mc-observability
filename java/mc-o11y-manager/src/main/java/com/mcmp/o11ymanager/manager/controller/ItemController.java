package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.manager.facade.ItemFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}/item")
@Tag(name = "[Manager] Monitoring VM Item Management")
public class ItemController {
    private final ItemFacadeService itemFacadeService;

    @GetMapping
    @Operation(
            summary = "GetMonitoringItems",
            operationId = "GetMonitoringItems",
            description = "Retrieve monitoring item list")
    public ResBody<List<MonitoringItemDTO>> getItems(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId) {
        return new ResBody<>(itemFacadeService.getTelegrafItems(nsId, mciId, vmId));
    }

    @PostMapping
    @Operation(
            summary = "AddMonitoringItem",
            operationId = "AddMonitoringItem",
            description = "Add monitoring item")
    public ResBody<Void> postItem(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId,
            @RequestBody @Valid MonitoringItemRequestDTO dto) {
        itemFacadeService.addTelegrafPlugin(nsId, mciId, vmId, dto);
        return ResBody.success(null);
    }

    @PutMapping
    @Operation(
            summary = "UpdateMonitoringItem",
            operationId = "UpdateMonitoringItem",
            description = "Update monitoring item")
    public ResBody<Void> putItem(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId,
            @RequestBody @Valid MonitoringItemUpdateDTO dto) {
        itemFacadeService.updateTelegrafPlugin(nsId, mciId, vmId, dto);
        return ResBody.success(null);
    }

    @DeleteMapping("/{itemSeq}")
    @Operation(
            summary = "DeleteMonitoringItem",
            operationId = "DeleteMonitoringItem",
            description = "Delete monitoring item")
    public ResBody<Void> deleteItem(
            @Parameter(description = "nsId (e.g., ns-1)", example = "ns-1") @PathVariable
                    String nsId,
            @Parameter(description = "mciId (e.g., mci-1)", example = "mci-1") @PathVariable
                    String mciId,
            @Parameter(description = "vmId (e.g., vm-1)", example = "vm-1") @PathVariable
                    String vmId,
            @Parameter(description = "Item Seq (e.g., 1)", example = "1") @PathVariable
                    Long itemSeq) {
        itemFacadeService.deleteTelegrafPlugin(nsId, mciId, vmId, itemSeq);
        return ResBody.success(null);
    }
}
