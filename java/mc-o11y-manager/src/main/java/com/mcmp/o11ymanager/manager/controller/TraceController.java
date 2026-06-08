package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.dto.trace.TraceResponseDto;
import com.mcmp.o11ymanager.manager.facade.TraceFacadeService;
import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/o11y/trace")
@Tag(name = "[Manager] Monitoring Trace")
public class TraceController {

    private final TraceFacadeService traceFacadeService;

    @GetMapping("/search")
    @Operation(
            summary = "TraceSearch",
            operationId = "TraceSearch",
            description =
                    "Search Tempo trace summaries. Provide a raw TraceQL `query`, or filter with"
                            + " `service` / `keyword` over a time window.")
    public ResBody<List<TraceResponseDto.TraceSummary>> searchTraces(
            @Parameter(
                            description =
                                    "Raw TraceQL (e.g. { resource.service.name=\"svc\" }). Overrides service/keyword.")
                    @RequestParam(required = false)
                    String query,
            @Parameter(description = "Service name filter (resource.service.name)")
                    @RequestParam(required = false)
                    String service,
            @Parameter(description = "Free-text keyword (matches span name or service name)")
                    @RequestParam(required = false)
                    String keyword,
            @Parameter(description = "Start time (RFC3339 or unix epoch). Defaults to 1h ago.")
                    @RequestParam(required = false)
                    String start,
            @Parameter(description = "End time (RFC3339 or unix epoch). Defaults to now.")
                    @RequestParam(required = false)
                    String end,
            @Parameter(description = "Maximum number of traces") @RequestParam(defaultValue = "100")
                    int limit) {

        return new ResBody<>(
                traceFacadeService.searchTraces(query, service, keyword, start, end, limit));
    }

    @GetMapping("/{traceId}")
    @Operation(
            summary = "TraceDetail",
            operationId = "TraceDetail",
            description = "Retrieve the flattened span list of a single trace (no flame graph).")
    public ResBody<TraceResponseDto.TraceDetail> getTrace(
            @Parameter(description = "Trace ID") @PathVariable String traceId) {

        return new ResBody<>(traceFacadeService.getTraceDetail(traceId));
    }
}
