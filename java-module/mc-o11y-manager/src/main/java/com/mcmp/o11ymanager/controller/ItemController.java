package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemRequestDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemUpdateDTO;
import com.mcmp.o11ymanager.exception.TelegrafConfigException;
import com.mcmp.o11ymanager.global.target.ResBody;
import com.mcmp.o11ymanager.facade.ItemFacadeService;
import org.springframework.http.ResponseEntity;
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


//    @GetMapping
//    public ResponseEntity<ResBody<List<MonitoringItemDTO>>> getItems2(
//            @PathVariable String nsId,
//            @PathVariable String mciId,
//            @PathVariable String targetId
//    ) {
//        try {
//            List<MonitoringItemDTO> items = itemFacadeService.getTelegrafItems(nsId, mciId, targetId);
//            return ResponseEntity.ok(ResBody.success(items));
//        } catch (TelegrafConfigException e) {
//            ResBody<List<MonitoringItemDTO>> errorResponse = ResBody.error(e.getResponseCode(), e.getMessage());
//            return ResponseEntity.status(e.getResponseCode().getHttpStatus()).body(errorResponse);
//        }
//    }




//    @PostMapping
//    public ResponseEntity<ResBody<Object>> postItem(
//            @PathVariable String nsId,
//            @PathVariable String mciId,
//            @PathVariable String targetId,
//            @RequestBody @Valid MonitoringItemRequestDTO dto
//    ) {
//        try {
//            itemFacadeService.addTelegrafPlugin(nsId, mciId, targetId, dto);
//            return ResponseEntity.ok(ResBody.success(null));
//        } catch (TelegrafConfigException e) {
//            ResBody<Object> errorResponse = ResBody.error(e.getResponseCode(), e.getMessage());
//            return ResponseEntity.status(e.getResponseCode().getHttpStatus()).body(errorResponse);
//        }
//    }
//
//    @PutMapping
//    public ResponseEntity<ResBody<Object>> putItem(
//            @PathVariable String nsId,
//            @PathVariable String mciId,
//            @PathVariable String targetId,
//            @RequestBody @Valid MonitoringItemUpdateDTO dto
//    ) {
//        try {
//            itemFacadeService.updateTelegrafPlugin(nsId, mciId, targetId, dto);
//            return ResponseEntity.ok(ResBody.success(null));
//        } catch (TelegrafConfigException e) {
//            ResBody<Object> errorResponse = ResBody.error(e.getResponseCode(), e.getMessage());
//            return ResponseEntity.status(e.getResponseCode().getHttpStatus()).body(errorResponse);
//        }
//    }
//
//    @DeleteMapping("/{itemSeq}")
//    public ResponseEntity<ResBody<Void>> deleteItem(
//            @PathVariable String nsId,
//            @PathVariable String mciId,
//            @PathVariable String targetId,
//            @PathVariable Long itemSeq
//    ) {
//        try {
//            itemFacadeService.deleteTelegrafPlugin(nsId, mciId, targetId, itemSeq);
//            return ResponseEntity.ok(ResBody.success(null));
//        } catch (TelegrafConfigException e) {
//            ResBody<Void> errorResponse = ResBody.error(e.getResponseCode(), e.getMessage());
//            return ResponseEntity.status(e.getResponseCode().getHttpStatus()).body(errorResponse);
//        }
//    }
}
