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
            @Parameter(
                            description =
                                    "Trace scope: framework (o11y platform) | vm (target VMs) | all")
                    @RequestParam(required = false)
                    String scope,
            @Parameter(description = "Start time (RFC3339 or unix epoch). Defaults to 1h ago.")
                    @RequestParam(required = false)
                    String start,
            @Parameter(description = "End time (RFC3339 or unix epoch). Defaults to now.")
                    @RequestParam(required = false)
                    String end,
            @Parameter(description = "Maximum number of traces") @RequestParam(defaultValue = "100")
                    int limit) {

        return new ResBody<>(
                traceFacadeService.searchTraces(query, service, keyword, scope, start, end, limit));
    }

    @GetMapping("/services")
    @Operation(
            summary = "TraceServiceList",
            operationId = "TraceServiceList",
            description =
                    "List service.name values in Tempo, optionally narrowed by scope"
                            + " (framework | vm | all). Drives the UI service dropdown.")
    public ResBody<List<String>> getServices(
            @Parameter(description = "Trace scope: framework | vm | all")
                    @RequestParam(required = false)
                    String scope) {

        return new ResBody<>(traceFacadeService.getServiceNames(scope));
    }

    @GetMapping("/attributes")
    @Operation(
            summary = "TraceAttributeList",
            operationId = "TraceAttributeList",
            description =
                    "List the attribute names Tempo saw in a window, written as TraceQL uses them"
                            + " (resource.x, span.x, intrinsics bare). Drives the RCA trace scope picker.")
    public ResBody<List<String>> getAttributeNames(
            @Parameter(
                            description =
                                    "TraceQL spanset narrowing the spans (e.g. { resource.ns_id=\"ns-1\" })")
                    @RequestParam(required = false)
                    String query,
            @Parameter(description = "Start time (RFC3339 or unix epoch). Defaults to 1h ago.")
                    @RequestParam(required = false)
                    String start,
            @Parameter(description = "End time (RFC3339 or unix epoch). Defaults to now.")
                    @RequestParam(required = false)
                    String end) {

        return new ResBody<>(traceFacadeService.getAttributeNames(query, start, end));
    }

    @GetMapping("/attributes/{attribute}/values")
    @Operation(
            summary = "TraceAttributeValueList",
            operationId = "TraceAttributeValueList",
            description =
                    "List the values one attribute takes in a window, each with its Tempo type,"
                            + " optionally narrowed by a spanset.")
    public ResBody<List<TraceResponseDto.AttributeValue>> getAttributeValues(
            @Parameter(
                            description =
                                    "Qualified attribute (e.g. resource.service.name, span.http.route, status)")
                    @PathVariable
                    String attribute,
            @Parameter(
                            description =
                                    "TraceQL spanset narrowing the spans (e.g. { resource.ns_id=\"ns-1\" })")
                    @RequestParam(required = false)
                    String query,
            @Parameter(description = "Start time (RFC3339 or unix epoch). Defaults to 1h ago.")
                    @RequestParam(required = false)
                    String start,
            @Parameter(description = "End time (RFC3339 or unix epoch). Defaults to now.")
                    @RequestParam(required = false)
                    String end) {

        return new ResBody<>(traceFacadeService.getAttributeValues(attribute, query, start, end));
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
