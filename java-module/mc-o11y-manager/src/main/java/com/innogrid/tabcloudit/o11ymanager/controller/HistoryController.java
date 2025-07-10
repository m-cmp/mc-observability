package com.innogrid.tabcloudit.o11ymanager.controller;

import com.innogrid.tabcloudit.o11ymanager.dto.common.SuccessResponse;
import com.innogrid.tabcloudit.o11ymanager.dto.history.HistoryListDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.history.HistoryResponseDTO;
import com.innogrid.tabcloudit.o11ymanager.facade.HistoryFacadeService;
import com.innogrid.tabcloudit.o11ymanager.global.aspect.request.RequestInfo;
import com.innogrid.tabcloudit.o11ymanager.global.annotation.HistoryInfoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/history")
public class HistoryController {
    private final HistoryFacadeService historyFacadeService;
    private final RequestInfo requestInfo;

    @Tag(name="History API", description = "이력 조회 API")
    @Operation(
            summary = "이력 리스트 조회",
            description = "모든 이력을 조회합니다.")
    @HistoryInfoResponse
    @GetMapping("/list")
    public ResponseEntity<SuccessResponse<List<HistoryListDTO>>> getList() {
        return SuccessResponse.of(requestInfo.getRequestId(), historyFacadeService.list());
    }

    @Tag(name="History API", description = "이력 조회 API")
    @Operation(
            summary = "단일 이력 조회",
            description = "특정 ID를 가진 이력을 조회합니다."
    )
    @HistoryInfoResponse
    @GetMapping("/{historyId}")
    public ResponseEntity<SuccessResponse<HistoryResponseDTO>> getHistory(@Parameter(description = "이력 ID", example = "9185c8a9-f155-4858-a405-050a6184e6da") @PathVariable String historyId) {
        return SuccessResponse.of(requestInfo.getRequestId(), historyFacadeService.findByIdWithHost(historyId));
    }
}
