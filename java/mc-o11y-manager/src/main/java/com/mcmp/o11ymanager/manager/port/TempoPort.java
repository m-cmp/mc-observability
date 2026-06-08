package com.mcmp.o11ymanager.manager.port;

import com.mcmp.o11ymanager.manager.dto.trace.TraceResponseDto;
import java.util.List;

/**
 * Interface (port) for interacting with the Tempo trace storage. Enables dependency inversion from
 * the domain layer to the infrastructure layer.
 */
public interface TempoPort {

    /**
     * Search trace summaries via a TraceQL expression within a time window.
     *
     * @param traceQl TraceQL expression (e.g. {@code { resource.service.name="svc" }})
     * @param limit maximum number of traces to return
     * @param startSec window start (unix seconds, optional)
     * @param endSec window end (unix seconds, optional)
     * @return trace summary list
     */
    List<TraceResponseDto.TraceSummary> searchTraces(
            String traceQl, Integer limit, Long startSec, Long endSec);

    /**
     * Fetch a single trace and flatten it into a span list.
     *
     * @param traceId trace ID
     * @return trace detail with span rows
     */
    TraceResponseDto.TraceDetail getTraceDetail(String traceId);
}
