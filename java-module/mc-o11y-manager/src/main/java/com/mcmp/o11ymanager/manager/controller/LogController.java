package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.log.LabelResultDto;
import com.mcmp.o11ymanager.manager.dto.log.LogSummaryDto;
import com.mcmp.o11ymanager.manager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.manager.facade.LogFacadeService;
import lombok.RequiredArgsConstructor;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/log")
public class LogController {

  private final LogFacadeService logFacadeService;


  @GetMapping("/query_range")
  public ResBody<LogSummaryDto.ResultDto> queryRangeLogs(
      @RequestParam String query,
      @RequestParam String start,
      @RequestParam String end,
      @RequestParam int limit,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String interval,
      @RequestParam(required = false) String step,
      @RequestParam(required = false) String since) {

    return new ResBody<>(
        logFacadeService.getRangeLogs(query, start, end, limit, direction, interval, step, since));
  }

  @GetMapping("/labels")
  public ResBody<LabelResultDto.LabelsDto> getLabels(
      @RequestParam(required = false) String start,
      @RequestParam(required = false) String end,
      @RequestParam(required = false) String query) {

    return new ResBody<>(logFacadeService.getLabelResult(start, end, query).getResult());
  }

  @GetMapping("/labels/{label}/values")
  public ResBody<LabelResultDto.LabelValuesDto> getLabelValues(
      @PathVariable String label,
      @RequestParam(required = false) String start,
      @RequestParam(required = false) String end,
      @RequestParam(required = false) String since,
      @RequestParam(required = false) String query) {

    return new ResBody<>(
        logFacadeService.getLabelValuesResult(label, start, end, since, query).getResult());
  }

  @GetMapping("/log_volumes")
  public ResBody<LogVolumeResponseDto> getLogVolumes(
      @RequestParam String query,
      @RequestParam String start,
      @RequestParam String end,
      @RequestParam(required = false) Integer limit) {

    return new ResBody<>(logFacadeService.getLogVolumes(query, start, end, limit));
  }
}
