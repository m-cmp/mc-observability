package com.innogrid.tabcloudit.o11ymanager.controller;

import com.innogrid.tabcloudit.o11ymanager.dto.host.AgentDTO;
import com.innogrid.tabcloudit.o11ymanager.dto.common.SuccessResponse;
import com.innogrid.tabcloudit.o11ymanager.dto.host.*;
import com.innogrid.tabcloudit.o11ymanager.enums.Agent;
import com.innogrid.tabcloudit.o11ymanager.facade.AgentFacadeService;
import com.innogrid.tabcloudit.o11ymanager.facade.FileFacadeService;
import com.innogrid.tabcloudit.o11ymanager.facade.FluentBitConfigFacadeService;
import com.innogrid.tabcloudit.o11ymanager.facade.FluentBitFacadeService;
import com.innogrid.tabcloudit.o11ymanager.facade.HostFacadeService;
import com.innogrid.tabcloudit.o11ymanager.facade.TelegrafConfigFacadeService;
import com.innogrid.tabcloudit.o11ymanager.facade.TelegrafFacadeService;
import com.innogrid.tabcloudit.o11ymanager.global.annotation.AuthorizationHeader;
import com.innogrid.tabcloudit.o11ymanager.global.annotation.CommonErrorResponse;
import com.innogrid.tabcloudit.o11ymanager.global.annotation.CommonSuccessResponse;
import com.innogrid.tabcloudit.o11ymanager.global.aspect.request.RequestInfo;
import com.innogrid.tabcloudit.o11ymanager.infrastructure.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/host")
public class AgentController {

  private final AgentFacadeService agentFacadeService;
  private final RequestInfo requestInfo;
  private final TelegrafConfigFacadeService telegrafConfigService;
  private final FluentBitConfigFacadeService fluentBitConfigService;
  private final TelegrafFacadeService telegrafFacadeService;
  private final FileFacadeService fileFacadeService;
  private final HostFacadeService hostFacadeService;
  private final FluentBitFacadeService fluentBitFacadeService;

  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 에이전트 설치",
      description = "호스트에 에이전트를 설치합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/install")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> installAgent(@RequestBody AgentDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result;
    result = agentFacadeService.install(request, requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
          summary = "호스트 에이전트 업데이트",
          description = "호스트의 에이전트를 업데이트합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/update")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> updateAgent(@RequestBody AgentDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result;
    result = agentFacadeService.update(request, requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 에이전트 삭제",
      description = "호스트의 에이전트를 삭제합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/uninstall")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> uninstallAgent(
      @RequestBody AgentDTO request, @Parameter(hidden = true) @AuthorizationHeader String token) {

    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result;

    result = agentFacadeService.uninstall(requestInfo.getRequestId(), request, requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }


  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 Telegraf 활성화",
      responses = {
          @ApiResponse(responseCode = "200", description = "Telegraf 활성화 성공",
              content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
      }
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/enable/telegraf")

  public ResponseEntity<SuccessResponse<List<ResultDTO>>> enableTelegraf(
      @RequestBody HostIDsDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result =
        telegrafFacadeService.enable(request.getHost_id_list(), requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }


  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 Fluent-Bit 활성화",
      responses = {
          @ApiResponse(responseCode = "200", description = "Fluent-Bit 활성화 성공",
              content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
      }
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/enable/fluent-bit")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> enableFluentBit(
      @RequestBody HostIDsDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result =
        fluentBitFacadeService.enable(request.getHost_id_list(), requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 Telegraf 비활성화",
      responses = {
          @ApiResponse(responseCode = "200", description = "Telegraf 비활성화 성공",
              content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
      }
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/disable/telegraf")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> disableTelegraf(
      @RequestBody HostIDsDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result =
        telegrafFacadeService.disable(request.getHost_id_list(), requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 Fluent-Bit 비활성화",
      responses = {
          @ApiResponse(responseCode = "200", description = "Fluent-Bit 비활성화 성공",
              content = @Content(schema = @Schema(implementation = SuccessResponse.class)))
      }
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/disable/fluent-bit")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> disableFluentBit(
      @RequestBody HostIDsDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {

    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result = fluentBitFacadeService.disable(request.getHost_id_list(),
        requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 Telegraf 재시작",
      description = "호스트의 Telegraf를 재시작 합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/restart/telegraf")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> restartTelegraf(
      @RequestBody HostIDsDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result =
        telegrafFacadeService.restart(request.getHost_id_list(), requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent API", description = "에이전트 관리 API")
  @Operation(
      summary = "호스트 Fluent-Bit 재시작",
      description = "호스트의 Fluent-Bit를 재시작 합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/restart/fluent-bit")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> restartFluentBit(
      @RequestBody HostIDsDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);

    List<ResultDTO> result = fluentBitFacadeService.restart(request.getHost_id_list(),
        requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Telegraf config 템플릿 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/agent/config/template/file/telegraf")
  public ResponseEntity<SuccessResponse<ConfigResponseDTO>> getTelegrafConfigTemplate(
      @RequestParam String path
  ) {
    ConfigResponseDTO result = telegrafConfigService.getTelegrafConfigTemplate(path);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Fluent-Bit config 템플릿 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/agent/config/template/file/fluent-bit")
  public ResponseEntity<SuccessResponse<ConfigResponseDTO>> getFluentBitConfigTemplate(
      @RequestParam String path
  ) {
    ConfigResponseDTO result = fluentBitConfigService.getFluentBitConfigTemplate(path);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Telegraf config 템플릿 파일 목록 조회",
      description = "Telegraf 설정 템플릿 파일 목록을 조회합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/agent/config/template/files/telegraf")
  public ResponseEntity<SuccessResponse<ConfigTemplateFileListResponseDTO>> getTelegrafConfigTemplateFileList() {

    ConfigTemplateFileListResponseDTO result = fileFacadeService.getTelegrafTemplateFileList();

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Fluent-Bit config 템플릿 파일 목록 조회",
      description = "Fluent-Bit 설정 템플릿 파일 목록을 조회합니다."
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/agent/config/template/files/fluent-bit")
  public ResponseEntity<SuccessResponse<ConfigTemplateFileListResponseDTO>> getFluentBitConfigTemplateFileList() {

    ConfigTemplateFileListResponseDTO result = fileFacadeService.getFluentBitTemplateFileList();

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Telegraf config 업데이트"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/config/update/telegraf")


  public ResponseEntity<SuccessResponse<List<ResultDTO>>> updateTelegrafConfig(
      @RequestBody ConfigDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);
    List<ResultDTO> result;
    AgentDTO agentDTO = new AgentDTO(true, false, request.getHost_id_list());

    result = agentFacadeService.updateTelegrafConfig(requestInfo.getRequestId(), agentDTO,
        request, requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Fluent-Bit config 업데이트"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/agent/config/update/fluent-bit")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> updateFluentBitConfig(
      @RequestBody ConfigDTO request,
      @Parameter(hidden = true) @AuthorizationHeader String token) {

    String requestUserId = JwtUtil.getRequestUserId(token);
    List<ResultDTO> result;
    AgentDTO agentDTO = new AgentDTO(false, true, request.getHost_id_list());

    result = agentFacadeService.updateFluentbitConfig(requestInfo.getRequestId(), agentDTO,
        request, requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Telegraf config 롤백"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/{id}/agent/config/rollback/telegraf")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> rollbackTelegrafConfig(
      @PathVariable String id,
      @RequestParam String commitHash,
      @Parameter(hidden = true) @AuthorizationHeader String token) {

    String requestUserId = JwtUtil.getRequestUserId(token);
    List<ResultDTO> result;
    String[] ids = {id};

    result = agentFacadeService.rollbackTelegrafConfig(List.of(ids), commitHash, requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Fluent-Bit config 롤백"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @PostMapping("/{id}/agent/config/rollback/fluent-bit")
  public ResponseEntity<SuccessResponse<List<ResultDTO>>> rollbackFluentBitConfig(
      @PathVariable String id,
      @RequestParam String commitHash,
      @Parameter(hidden = true) @AuthorizationHeader String token) {
    String requestUserId = JwtUtil.getRequestUserId(token);
    List<ResultDTO> result;
    String[] ids = {id};

    result = agentFacadeService.rollbackFluentbitConfig(List.of(ids), commitHash, requestUserId);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Telegraf config 이력 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/history/telegraf")
  public ResponseEntity<SuccessResponse<List<ConfigHistoryDTO>>> getTelegrafConfigHistory(
      @PathVariable String id,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer row) {

    List<ConfigHistoryDTO> configHistory = hostFacadeService.getHostConfigHistory(
        requestInfo.getRequestId(), Agent.TELEGRAF, id, page, row);

    return SuccessResponse.of(requestInfo.getRequestId(), configHistory);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "Fluent-Bit config 이력 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/history/fluent-bit")
  public ResponseEntity<SuccessResponse<List<ConfigHistoryDTO>>> getFluentBitConfigHistory(
      @PathVariable String id,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer row) {

    List<ConfigHistoryDTO> configHistory = hostFacadeService.getHostConfigHistory(
        requestInfo.getRequestId(), Agent.FLUENT_BIT, id, page, row);

    return SuccessResponse.of(requestInfo.getRequestId(), configHistory);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "현재 Telegraf 설정 파일 목록 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/files/telegraf")
  public ResponseEntity<SuccessResponse<ConfigFileListResponseDTO>> getCurrentTelegrafConfigFileList(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id) {

    ConfigFileListResponseDTO result = telegrafConfigService.getTelegrafConfigFileList(
        requestInfo.getRequestId(), id, null);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "특정 commit 시점의 Telegraf 설정 파일 목록 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/files/telegraf/{commitHash}")
  public ResponseEntity<SuccessResponse<ConfigFileListResponseDTO>> getTelegrafConfigFileList(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id,
      @Parameter(description = "Commit Hash") @PathVariable String commitHash) {

    ConfigFileListResponseDTO result = telegrafConfigService.getTelegrafConfigFileList(
        requestInfo.getRequestId(), id, commitHash);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "현재 Fluent-Bit 설정 파일 목록 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/files/fluent-bit")
  public ResponseEntity<SuccessResponse<ConfigFileListResponseDTO>> getCurrentFluentBitConfigFileList(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id) {

    ConfigFileListResponseDTO result = fluentBitConfigService.getFluentBitConfigFileList(
        requestInfo.getRequestId(), id, null);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "특정 commit 시점의 Fluent-Bit 설정 파일 목록 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/files/fluent-bit/{commitHash}")
  public ResponseEntity<SuccessResponse<ConfigFileListResponseDTO>> getFluentBitConfigFileList(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id,
      @Parameter(description = "Commit Hash") @PathVariable String commitHash) {

    ConfigFileListResponseDTO result = fluentBitConfigService.getFluentBitConfigFileList(
        requestInfo.getRequestId(), id, commitHash);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "현재 Telegraf 설정 파일 내용 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/file/content/telegraf")
  public ResponseEntity<SuccessResponse<ConfigFileContentResponseDTO>> getCurrentTelegrafConfigFileContent(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id,
      @Parameter(description = "파일 경로") @RequestParam String path) {

    ConfigFileContentResponseDTO result = telegrafConfigService.getTelegrafConfigContent(
        requestInfo.getRequestId(), id, null, path);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "특정 commit 시점의 Telegraf 설정 파일 내용 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/file/content/telegraf/{commitHash}")
  public ResponseEntity<SuccessResponse<ConfigFileContentResponseDTO>> getTelegrafConfigFileContent(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id,
      @Parameter(description = "Commit Hash") @PathVariable String commitHash,
      @Parameter(description = "파일 경로") @RequestParam String path) {

    ConfigFileContentResponseDTO result = telegrafConfigService.getTelegrafConfigContent(
        requestInfo.getRequestId(), id, commitHash, path);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "현재 Fluent-Bit 설정 파일 내용 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/file/content/fluent-bit")
  public ResponseEntity<SuccessResponse<ConfigFileContentResponseDTO>> getCurrentFluentBitConfigFileContent(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id,
      @Parameter(description = "파일 경로") @RequestParam String path) {

    ConfigFileContentResponseDTO result = fluentBitConfigService.getFluentBitConfigContent(
        requestInfo.getRequestId(), id, null, path);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }

  @Tag(name = "Agent Config API", description = "에이전트 설정 관리 API")
  @Operation(
      summary = "특정 commit 시점의 Fluent-Bit 설정 파일 내용 조회"
  )
  @CommonSuccessResponse
  @CommonErrorResponse
  @GetMapping("/{id}/agent/config/file/content/fluent-bit/{commitHash}")
  public ResponseEntity<SuccessResponse<ConfigFileContentResponseDTO>> getFluentBitConfigFileContent(
      @Parameter(description = "호스트 ID", example = "8b3558e7-b4b8-460d-960a-10bc57b8ef6b") @PathVariable String id,
      @Parameter(description = "Commit Hash") @PathVariable String commitHash,
      @Parameter(description = "파일 경로") @RequestParam String path) {

    ConfigFileContentResponseDTO result = fluentBitConfigService.getFluentBitConfigContent(
        requestInfo.getRequestId(), id, commitHash, path);

    return SuccessResponse.of(requestInfo.getRequestId(), result);
  }
}
