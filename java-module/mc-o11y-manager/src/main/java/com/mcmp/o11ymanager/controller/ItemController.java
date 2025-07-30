package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.global.target.ResBody;
import com.mcmp.o11ymanager.service.TelegrafConfigService;
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
    private final TelegrafConfigService telegrafConfigService;

    @GetMapping
    public ResBody<List<MonitoringItemDTO>> getItems(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String targetId
    ) {
        // TODO: userName 파라미터 필요 - 실제 구현시 TargetEntity에서 가져와야 함
        String userName = "root"; // 임시값
        List<MonitoringItemDTO> items = telegrafConfigService.getTelegrafItems(nsId, mciId, targetId, userName);
        return new ResBody<>(items);
    }

    @PostMapping
    public ResBody<Object> postItem(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String targetId,
            @RequestBody @Valid MonitoringItemRequestDTO dto
    ) {
        // TODO: Implement monitoring item service
        return new ResBody<>();
    }

    @PutMapping
    public ResBody<Object> putItem(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String targetId,
            @RequestBody @Valid MonitoringItemUpdateDTO dto
    ) {
        // TODO: Implement monitoring item service
        return new ResBody<>();
    }

    @DeleteMapping
    public ResBody<Void> deleteItem(
            @PathVariable String nsId,
            @PathVariable String mciId,
            @PathVariable String targetId,
            @PathVariable Long itemSeq
    ) {
        // TODO: Implement monitoring item service
        return new ResBody<>();
    }
}
