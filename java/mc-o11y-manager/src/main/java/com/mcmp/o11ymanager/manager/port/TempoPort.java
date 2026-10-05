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

    /**
     * List distinct service names known to Tempo.
     *
     * @return service.name values
     */
    List<String> getServiceNames();

    /**
     * Attribute names seen in a window, written the way TraceQL uses them: {@code resource.x},
     * {@code span.x}, and intrinsics ({@code status}, {@code kind}, ...) bare.
     *
     * @param traceQl spanset narrowing the spans looked at (optional)
     */
    List<String> getAttributeNames(String traceQl, Long startSec, Long endSec);

    /**
     * Distinct values one attribute takes in a window, each with the type Tempo reported for it.
     *
     * @param attribute qualified attribute name (e.g. {@code resource.service.name})
     * @param traceQl spanset narrowing the spans looked at (optional)
     */
    List<TraceResponseDto.AttributeValue> getAttributeValues(
            String attribute, String traceQl, Long startSec, Long endSec);
}
