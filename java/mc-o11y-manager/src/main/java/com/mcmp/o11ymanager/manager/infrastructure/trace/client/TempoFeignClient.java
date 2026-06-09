package com.mcmp.o11ymanager.manager.infrastructure.trace.client;

import com.mcmp.o11ymanager.manager.infrastructure.log.client.FeignLogConfig;
import com.mcmp.o11ymanager.manager.infrastructure.trace.dto.TempoSearchResponseDto;
import com.mcmp.o11ymanager.manager.infrastructure.trace.dto.TempoServiceValuesDto;
import com.mcmp.o11ymanager.manager.infrastructure.trace.dto.TempoTraceDto;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tempo HTTP query surface used by the trace facade. Only the subset we render — TraceQL search for
 * the list view and a single-trace fetch for the span detail view.
 */
@FeignClient(name = "TempoFeignClient", url = "${tempo.url}", configuration = FeignLogConfig.class)
public interface TempoFeignClient {

    /**
     * TraceQL-backed search. {@code q} is a raw TraceQL expression (e.g. {@code {
     * resource.service.name="svc" }}). Returns trace IDs + root service/name + duration so the list
     * renders without a fetch per row.
     */
    @GetMapping(value = "${tempo.endpoints.search}")
    Optional<TempoSearchResponseDto> search(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "start", required = false) Long start,
            @RequestParam(value = "end", required = false) Long end);

    /** Full OTLP-shaped payload of a single trace (resource + scope + spans). */
    @GetMapping(value = "${tempo.endpoints.trace}/{traceId}")
    Optional<TempoTraceDto> getTrace(@PathVariable("traceId") String traceId);

    /** Distinct service.name values ingested by Tempo — drives the UI service dropdown. */
    @GetMapping(value = "${tempo.endpoints.serviceValues}")
    Optional<TempoServiceValuesDto> getServiceNames();
}
