package com.mcmp.o11ymanager.controller;

import com.mcmp.o11ymanager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.facade.LogFacadeService;
import com.mcmp.o11ymanager.global.aspect.request.RequestInfo;
import com.mcmp.o11ymanager.dto.common.SuccessResponse;
import com.mcmp.o11ymanager.enums.ResponseCode;
import com.mcmp.o11ymanager.dto.log.LabelResultDto;
import com.mcmp.o11ymanager.dto.log.LogSummaryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Loki 로그 컨트롤러
 * 프레젠테이션 계층에서 API 요청을 처리
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/log")
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

        LogSummaryDto.ResultDto logs = logFacadeService.getRangeLogs(query, start, end, limit, direction,
                interval, step, since);

        return SuccessResponse.of(
                requestInfo.getRequestId(),
                logs,
                ResponseCode.OK,
                "로그 기간 데이터 읽기 성공하였습니다."
        );
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
                "레이블 목록 조회 성공하였습니다."
        );
    }

    @GetMapping("/labels/{label}/values")
    public ResponseEntity<SuccessResponse<LabelResultDto.LabelValuesDto>> getLabelValues(
            @PathVariable String label,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String query) {

        LabelResultDto.LabelValuesResultDto labelValues = logFacadeService.getLabelValuesResult(label, start, end, since, query);

        return SuccessResponse.of(
                requestInfo.getRequestId(),
                labelValues.getResult(),
                ResponseCode.OK,
                "레이블 값 목록 조회 성공하였습니다."
        );
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
                "로그 볼륨 데이터 조회 성공하였습니다."
        );
    }
}