package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.log.LabelResultDto;
import com.mcmp.o11ymanager.manager.dto.log.LogSummaryDto;
import com.mcmp.o11ymanager.manager.dto.log.LogVolumeResponseDto;
import com.mcmp.o11ymanager.manager.facade.LogFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/log")
@Tag(name = "[Manager] Monitoring Log")
public class LogController {

    private final LogFacadeService logFacadeService;

    @GetMapping("/query_range")
    @Operation(
            summary = "LogRangeQuery",
            operationId = "LogRangeQuery",
            description = "Retrieve log data for a specific query within a given time range.")
    public ResBody<LogSummaryDto.ResultDto> queryRangeLogs(
            @Parameter(description = "Query string (e.g., {NS_ID=\"test01\"})") @RequestParam
                    String query,
            @Parameter(description = "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)")
                    @RequestParam(required = true)
                    String start,
            @Parameter(description = "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)")
                    @RequestParam(required = true)
                    String end,
            @Parameter(description = "Maximum number of entries") @RequestParam int limit,
            @Parameter(description = "Direction (FORWARD/BACKWARD)") @RequestParam(required = false)
                    String direction,
            @Parameter(description = "Interval (e.g. 1m)") @RequestParam(required = false)
                    String interval,
            @Parameter(description = "Step (e.g. 30s)") @RequestParam(required = false) String step,
            @Parameter(description = "Since duration (e.g. 1h)") @RequestParam(required = false)
                    String since) {

        return new ResBody<>(
                logFacadeService.getRangeLogs(
                        query, start, end, limit, direction, interval, step, since));
    }

    @GetMapping("/labels")
    @Operation(
            summary = "LogLabelsQuery",
            operationId = "LogLabelsQuery",
            description = "Retrieve the list of label keys provided by Loki.")
    public ResBody<LabelResultDto.LabelsDto> getLabels(
            @Parameter(description = "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)")
                    @RequestParam(required = false)
                    String start,
            @Parameter(description = "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)")
                    @RequestParam(required = false)
                    String end,
            @Parameter(description = "Query string (e.g., {NS_ID=\"test01\"})")
                    @RequestParam(required = false)
                    String query) {

        return new ResBody<>(logFacadeService.getLabelResult(start, end, query).getResult());
    }

    @GetMapping("/labels/{label}/values")
    @Operation(
            summary = "LabelValueQuery",
            operationId = "LabelValueQuery",
            description = "Retrieve the list of values for a specific label key.")
    public ResBody<LabelResultDto.LabelValuesDto> getLabelValues(
            @Parameter(description = "Label key (e.g., NS_ID, MCI_ID, service)") @PathVariable
                    String label,
            @Parameter(description = "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)")
                    @RequestParam(required = false)
                    String start,
            @Parameter(description = "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)")
                    @RequestParam(required = false)
                    String end,
            @Parameter(description = "Since duration (e.g., 1h)") @RequestParam(required = false)
                    String since,
            @Parameter(description = "Query string (e.g., {NS_ID=\"test01\"})")
                    @RequestParam(required = false)
                    String query) {

        return new ResBody<>(
                logFacadeService.getLabelValuesResult(label, start, end, since, query).getResult());
    }

    @GetMapping("/log_volumes")
    @Operation(
            summary = "LogVolumeQuery",
            operationId = "LogVolumeQuery",
            description = "Retrieve log volumes (metric time series data) for the given period.")
    public ResBody<LogVolumeResponseDto> getLogVolumes(
            @Parameter(description = "Query string (e.g., {NS_ID=\"test01\"})") @RequestParam
                    String query,
            @Parameter(description = "Start timestamp (RFC3339) (e.g., 2025-09-01T00:00:00Z)")
                    @RequestParam
                    String start,
            @Parameter(description = "End timestamp (RFC3339) (e.g., 2025-09-02T00:00:00Z)")
                    @RequestParam
                    String end,
            @Parameter(description = "Maximum series returned") @RequestParam(required = false)
                    Integer limit) {

        return new ResBody<>(logFacadeService.getLogVolumes(query, start, end, limit));
    }
}
