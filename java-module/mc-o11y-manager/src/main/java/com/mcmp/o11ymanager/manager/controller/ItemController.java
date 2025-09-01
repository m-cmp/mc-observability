package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.manager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.manager.global.target.ResBody;
import com.mcmp.o11ymanager.manager.facade.ItemFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/monitoring/{nsId}/{mciId}/target/{targetId}/item")
public class ItemController {
    private final ItemFacadeService itemFacadeService;


    @GetMapping
    public ResBody<List<MonitoringItemDTO>> getItems(
        @PathVariable String nsId,
        @PathVariable String mciId,
        @PathVariable String targetId
    ) {
        return new ResBody<>(itemFacadeService.getTelegrafItems(nsId, mciId, targetId));
    }

    @PostMapping
    public ResBody<Void> postItem(
        @PathVariable String nsId,
        @PathVariable String mciId,
        @PathVariable String targetId,
        @RequestBody @Valid MonitoringItemRequestDTO dto
    ) {
        itemFacadeService.addTelegrafPlugin(nsId, mciId, targetId, dto);
        return ResBody.success(null);
    }


    @PutMapping
    public ResBody<Void> putItem(
        @PathVariable String nsId,
        @PathVariable String mciId,
        @PathVariable String targetId,
        @RequestBody @Valid MonitoringItemUpdateDTO dto
    ) {
        itemFacadeService.updateTelegrafPlugin(nsId, mciId, targetId, dto);
        return ResBody.success(null);
    }

    @DeleteMapping("/{itemSeq}")
    public ResBody<Void> deleteItem(
        @PathVariable String nsId,
        @PathVariable String mciId,
        @PathVariable String targetId,
        @PathVariable Long itemSeq
    ) {
        itemFacadeService.deleteTelegrafPlugin(nsId, mciId, targetId, itemSeq);
        return ResBody.success(null);
    }

}
