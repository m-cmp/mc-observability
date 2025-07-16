package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.common.SuccessResponse;
import com.mcmp.o11ymanager.dto.history.HistoryResponseDTO;
import com.mcmp.o11ymanager.dto.host.HostCreateDTO;
import com.mcmp.o11ymanager.dto.host.HostIDsDTO;
import com.mcmp.o11ymanager.dto.host.HostResponseDTO;
import com.mcmp.o11ymanager.dto.host.HostStatisticsResponseDTO;
import com.mcmp.o11ymanager.dto.host.HostUpdateDTO;
import com.mcmp.o11ymanager.dto.host.ResultDTO;
import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetUpdateDTO;
import com.mcmp.o11ymanager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.facade.HistoryFacadeService;
import com.mcmp.o11ymanager.facade.HostFacadeService;
import com.mcmp.o11ymanager.facade.TargetFacadeService;
import com.mcmp.o11ymanager.global.annotation.AuthorizationHeader;
import com.mcmp.o11ymanager.global.annotation.CommonErrorResponse;
import com.mcmp.o11ymanager.global.annotation.CommonSuccessResponse;
import com.mcmp.o11ymanager.global.annotation.HistoryInfoResponse;
import com.mcmp.o11ymanager.global.annotation.HostInfoResponse;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.global.target.ResBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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
public class TargetController { ;

  private final TargetFacadeService targetFacadeService;
  public ResBody<TargetDTO> getTarget(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String targetId
  ) {
    return new ResBody<>(targetFacadeService.getTarget(nsId, mciId, targetId));
  }

  @PutMapping("/{nsId}/{mciId}/target/{targetId}")
  public ResBody<TargetDTO> putTarget(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String targetId,
      @RequestBody TargetUpdateDTO dto
  ) {
    return new ResBody<>(targetFacadeService.putTarget(targetId, nsId, mciId, dto));
  }

  @DeleteMapping("/{nsId}/{mciId}/target/{targetId}")
  public ResBody<Void> deleteTarget(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String targetId
  ) {
    targetFacadeService.deleteTarget(targetId, nsId, mciId);
    return new ResBody<>();
  }

  @GetMapping("/{nsId}/{mciId}/target")
  public ResBody<TargetDTO> getTargetByNsMci(
      @PathVariable String nsId,
      @PathVariable String mciId
  ) {
    return new ResBody<>(targetFacadeService.getTargetsNsMci(nsId, mciId));
  }

  @GetMapping("/target")
  public ResBody<List<TargetDTO>> getAllTargets() {
    return new ResBody<>(targetFacadeService.getTargets());
  }

  @GetMapping("/ns")
  public ResBody<List<TumblebugNS.NS>> getNamespaceList() {
    return new ResBody<>(targetFacadeService.getNamespaceList());
  }


}
