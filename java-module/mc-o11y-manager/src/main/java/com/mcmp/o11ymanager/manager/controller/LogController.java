package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.common.SuccessResponse;
import com.mcmp.o11ymanager.manager.dto.log.LabelResultDto;
import com.mcmp.o11ymanager.manager.dto.log.LogSummaryDto;
import com.mcmp.o11ymanager.manager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.manager.enums.ResponseCode;
import com.mcmp.o11ymanager.manager.facade.LogFacadeService;
import com.mcmp.o11ymanager.manager.global.aspect.request.RequestInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/log")
public class LogController {

    private final LogFacadeService logFacadeService;
    private final RequestInfo requestInfo;

    @GetMapping("/query_range")
    public ResponseEntity<SuccessResponse<LogSummaryDto.ResultDto>> queryRangeLogs(
            @RequestParam String query,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam int limit,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) String interval,
            @RequestParam(required = false) String step,
            @RequestParam(required = false) String since) {

        LogSummaryDto.ResultDto logs =
                logFacadeService.getRangeLogs(
                        query, start, end, limit, direction, interval, step, since);

        return SuccessResponse.of(
                requestInfo.getRequestId(),
                logs,
                ResponseCode.OK,
                "Successfully retrieved log range data.");
    }

    @GetMapping("/labels")
    public ResponseEntity<SuccessResponse<LabelResultDto.LabelsDto>> getLabels(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String query) {

        LabelResultDto.LabelsResultDto labels = logFacadeService.getLabelResult(start, end, query);

        return SuccessResponse.of(
                requestInfo.getRequestId(),
                labels.getResult(),
                ResponseCode.OK,
                "Successfully retrieved label list.");
    }

    @GetMapping("/labels/{label}/values")
    public ResponseEntity<SuccessResponse<LabelResultDto.LabelValuesDto>> getLabelValues(
            @PathVariable String label,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String query) {

        LabelResultDto.LabelValuesResultDto labelValues =
                logFacadeService.getLabelValuesResult(label, start, end, since, query);

        return SuccessResponse.of(
                requestInfo.getRequestId(),
                labelValues.getResult(),
                ResponseCode.OK,
                "Successfully retrieved label value list.");
    }

    @GetMapping("/log_volumes")
    public ResponseEntity<SuccessResponse<LogVolumeResponseDto>> getLogVolumes(
            @RequestParam String query,
            @RequestParam String start,
            @RequestParam String end,
            @RequestParam(required = false) Integer limit) {

        LogVolumeResponseDto volumes = logFacadeService.getLogVolumes(query, start, end, limit);

        return SuccessResponse.of(
                requestInfo.getRequestId(),
                volumes,
                ResponseCode.OK,
                "Successfully retrieved log volume data.");
    }
}
