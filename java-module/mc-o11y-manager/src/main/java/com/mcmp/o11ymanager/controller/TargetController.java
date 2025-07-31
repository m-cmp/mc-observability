package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.target.TargetDTO;
import com.mcmp.o11ymanager.dto.target.TargetRequestDTO;
import com.mcmp.o11ymanager.dto.item.MonitoringItemDTO;
import com.mcmp.o11ymanager.dto.plugin.PluginDefDTO;
import com.mcmp.o11ymanager.exception.TelegrafConfigException;
import com.mcmp.o11ymanager.facade.TargetFacadeService;
import com.mcmp.o11ymanager.service.AgentPluginDefService;
import com.mcmp.o11ymanager.service.TelegrafConfigService;
import com.mcmp.o11ymanager.global.target.ResBody;
import org.springframework.http.ResponseEntity;
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
public class TargetController {

  private final TargetFacadeService targetFacadeService;
  private final AgentPluginDefService agentPluginDefService;
  private final TelegrafConfigService telegrafConfigService;

  @GetMapping("/{nsId}/{mciId}/target/{targetId}")
  public ResBody<TargetDTO> getTarget(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String targetId
  ) {
    return new ResBody<>(targetFacadeService.getTarget(nsId, mciId, targetId));
  }


  @PostMapping("/{nsId}/{mciId}/target/{targetId}")
  public ResBody<TargetDTO> postTarget(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String targetId,
      @RequestBody @Valid TargetRequestDTO dto
  ) {
    return new ResBody<>(targetFacadeService.postTarget(nsId, mciId, targetId, dto));
  }



  @PutMapping("/{nsId}/{mciId}/target/{targetId}")
  public ResBody<TargetDTO> putTarget(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String targetId,
      @RequestBody TargetRequestDTO dto
  ) {
    return new ResBody<>(targetFacadeService.putTarget(nsId, mciId, targetId, dto));
  }

  @DeleteMapping("/{nsId}/{mciId}/target/{targetId}")
  public ResBody<Void> deleteTarget(
      @PathVariable String nsId,
      @PathVariable String mciId,
      @PathVariable String targetId
  ) {
    targetFacadeService.deleteTarget(nsId, mciId,targetId);
    return new ResBody<>();
  }

  @GetMapping("/{nsId}/{mciId}/target")
  public ResBody<List<TargetDTO>> getTargetByNsMci(
      @PathVariable String nsId,
      @PathVariable String mciId
      ) {
    return new ResBody<>(targetFacadeService.getTargetsNsMci(nsId, mciId));
  }

  @GetMapping("/target")
  public ResBody<List<TargetDTO>> getAllTargets() {
    return new ResBody<>(targetFacadeService.getTargets());
  }

}
