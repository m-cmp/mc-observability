package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.manager.facade.ItemFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/{nsId}/{mciId}/vm/{vmId}/item")
public class ItemController {
    private final ItemFacadeService itemFacadeService;

    @GetMapping
    public ResBody<List<MonitoringItemDTO>> getItems(
            @PathVariable String nsId, @PathVariable String mciId, @PathVariable String vmId) {
        return new ResBody<>(itemFacadeService.getTelegrafItems(nsId, mciId, vmId));
    }

    @PostMapping
    public ResBody<Void> postItem(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @RequestBody @Valid MonitoringItemRequestDTO dto) {
        itemFacadeService.addTelegrafPlugin(nsId, mciId, vmId, dto);
        return ResBody.success(null);
    }

    @PutMapping
    public ResBody<Void> putItem(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @RequestBody @Valid MonitoringItemUpdateDTO dto) {
        itemFacadeService.updateTelegrafPlugin(nsId, mciId, vmId, dto);
        return ResBody.success(null);
    }

    @DeleteMapping("/{itemSeq}")
    public ResBody<Void> deleteItem(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String vmId,
            @PathVariable Long itemSeq) {
        itemFacadeService.deleteTelegrafPlugin(nsId, mciId, vmId, itemSeq);
        return ResBody.success(null);
    }
}
