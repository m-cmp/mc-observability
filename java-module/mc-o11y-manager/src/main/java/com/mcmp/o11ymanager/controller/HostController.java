package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.common.SuccessResponse;
import com.mcmp.o11ymanager.dto.history.HistoryResponseDTO;
import com.mcmp.o11ymanager.facade.HistoryFacadeService;
import com.mcmp.o11ymanager.facade.HostFacadeService;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.dto.host.*;

import java.util.List;

import com.mcmp.o11ymanager.global.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/host")
public class HostController {

  private final HostFacadeService hostFacadeService;
  private final RequestInfo requestInfo;
  private final HistoryFacadeService historyFacadeService;


  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "호스트 리스트 조회",
      description = "모든 호스트를 조회합니다."
  )
  @HostInfoResponse
  @GetMapping("/list")
  public ResponseEntity<SuccessResponse<List<HostResponseDTO>>> getList() {
    return SuccessResponse.of(requestInfo.getRequestId(), hostFacadeService.list());
  }

  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "단일 호스트 조회",
      description = "특정 호스트를 조회합니다."
  )
  @HostInfoResponse
  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<HostResponseDTO>> getHost(@PathVariable String id) {
    return SuccessResponse.of(requestInfo.getRequestId(), hostFacadeService.getHost(id));
  }

  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "상태 갱신 된 호스트 리스트 조회",
      description = "모든 호스트들의 상태를 즉시 갱신하여 호스트 리스트를 조회합니다."
  )
  @HostInfoResponse
  @GetMapping("/list/refresh")
  public ResponseEntity<SuccessResponse<List<HostResponseDTO>>> getListRefresh() {
    return SuccessResponse.of(requestInfo.getRequestId(), hostFacadeService.listHostRefresh());
  }

  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "호스트 상태 통계",
      description = "모든 호스트의 상태에 대한 통계를 제공합니다."
  )
  @HostInfoResponse
  @GetMapping("/statistics")
  public ResponseEntity<SuccessResponse<HostStatisticsResponseDTO>> getStatistics() {
    return SuccessResponse.of(requestInfo.getRequestId(), hostFacadeService.hostStatistics());
  }

  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "특정 호스트의 이력 조회",
      description = "특정 호스트의 이력을 조회합니다."
  )
  @HistoryInfoResponse
  @GetMapping("/{id}/history")
  public ResponseEntity<SuccessResponse<List<HistoryResponseDTO>>> getHistoryListOfHost(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id) {
    return SuccessResponse.of(requestInfo.getRequestId(), historyFacadeService.listHistory(id));

  }

  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "호스트 등록",
      description = "호스트를 등록합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/create")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> create(
      @Parameter(hidden = true) @AuthorizationHeader String token,
      @Valid @RequestBody List<HostCreateDTO> request) {
    return SuccessResponse.of(requestInfo.getRequestId(), hostFacadeService.create(request));
  }

  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "호스트 업데이트",
      description = "호스트의 정보를 업데이트 합니다."
  )
  @HostInfoResponse
  @PostMapping("/update")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> update(
      @Parameter(hidden = true) @AuthorizationHeader String token,
      @Valid @RequestBody HostUpdateDTO request) {

    List<ResultDTO> result = hostFacadeService.update(request);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Host API", description = "호스트 관리 API")
  @Operation(
      summary = "호스트 삭제",
      description = "호스트의 정보를 삭제 합니다."
  )
  @HostInfoResponse
  @PostMapping("/delete")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> delete(
      @Parameter(hidden = true) @AuthorizationHeader String token,
      @RequestBody HostIDsDTO request) {

    List<ResultDTO> result = hostFacadeService.delete(request);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

}
