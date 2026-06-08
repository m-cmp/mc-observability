package com.mcmp.o11ymanager.manager.infrastructure.trace.adapter;

import com.mcmp.o11ymanager.manager.dto.trace.TraceResponseDto;
import com.mcmp.o11ymanager.manager.infrastructure.trace.client.TempoFeignClient;
import com.mcmp.o11ymanager.manager.infrastructure.trace.dto.TempoSearchResponseDto;
import com.mcmp.o11ymanager.manager.infrastructure.trace.dto.TempoTraceDto;
import com.mcmp.o11ymanager.manager.port.TempoPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Maps Tempo's HTTP responses to the trace domain. Tempo failures (404 for unknown trace IDs,
 * connectivity issues) degrade to empty results rather than propagating 500s to the UI.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempoClientAdapter implements TempoPort {

    private final TempoFeignClient tempoFeignClient;

    @Override
    public List<TraceResponseDto.TraceSummary> searchTraces(
            String traceQl, Integer limit, Long startSec, Long endSec) {
        List<TempoSearchResponseDto.TraceSummary> raw;
        try {
            raw =
                    tempoFeignClient
                            .search(traceQl, limit, startSec, endSec)
                            .map(TempoSearchResponseDto::getTraces)
                            .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.warn("tempo search failed q={} err={}", traceQl, e.getMessage());
            return Collections.emptyList();
        }
        if (raw == null) {
            return Collections.emptyList();
        }
        return raw.stream()
                .map(
                        t ->
                                TraceResponseDto.TraceSummary.builder()
                                        .traceId(t.getTraceID())
                                        .rootService(t.getRootServiceName())
                                        .rootName(t.getRootTraceName())
                                        .startTimeMs(parseUnixNanoToMs(t.getStartTimeUnixNano()))
                                        .durationMs(
                                                t.getDurationMs() == null ? 0 : t.getDurationMs())
                                        .build())
                .sorted(
                        Comparator.comparingLong(TraceResponseDto.TraceSummary::getStartTimeMs)
                                .reversed())
                .collect(Collectors.toList());
    }

    @Override
    public TraceResponseDto.TraceDetail getTraceDetail(String traceId) {
        TempoTraceDto raw;
        try {
            raw = tempoFeignClient.getTrace(traceId).orElse(null);
        } catch (Exception e) {
            log.warn("tempo getTrace failed id={} err={}", traceId, e.getMessage());
            raw = null;
        }

        List<TraceResponseDto.SpanRow> rows = new ArrayList<>();
        if (raw != null && raw.getBatches() != null) {
            for (TempoTraceDto.Batch b : raw.getBatches()) {
                String svc = serviceFromResource(b.getResource());
                if (b.getScopeSpans() == null) {
                    continue;
                }
                for (TempoTraceDto.ScopeSpans ss : b.getScopeSpans()) {
                    if (ss.getSpans() == null) {
                        continue;
                    }
                    for (TempoTraceDto.Span sp : ss.getSpans()) {
                        long startNs = parseUnixNano(sp.getStartTimeUnixNano());
                        long endNs = parseUnixNano(sp.getEndTimeUnixNano());
                        // Truncate START to ms for display alignment; keep DURATION fractional so
                        // sub-ms spans (DB lookups, RPC calls) don't all collapse to 0/1ms.
                        long startMs = startNs / 1_000_000L;
                        double durationMs = Math.max(0, (endNs - startNs) / 1_000_000.0);
                        rows.add(
                                TraceResponseDto.SpanRow.builder()
                                        .spanId(sp.getSpanId())
                                        .parentSpanId(sp.getParentSpanId())
                                        .service(svc)
                                        .name(sp.getName())
                                        .kind(sp.getKind())
                                        .startTimeMs(startMs)
                                        .durationMs(durationMs)
                                        .attributes(flattenAttrs(sp.getAttributes()))
                                        .build());
                    }
                }
            }
            rows.sort(Comparator.comparingLong(TraceResponseDto.SpanRow::getStartTimeMs));
        }
        return TraceResponseDto.TraceDetail.builder().traceId(traceId).spans(rows).build();
    }

    // ---------- helpers ----------

    private static String serviceFromResource(TempoTraceDto.Resource r) {
        if (r == null || r.getAttributes() == null) {
            return "";
        }
        for (TempoTraceDto.Attribute a : r.getAttributes()) {
            if ("service.name".equals(a.getKey())) {
                return attrValueAsString(a.getValue());
            }
        }
        return "";
    }

    private static Map<String, String> flattenAttrs(List<TempoTraceDto.Attribute> attrs) {
        Map<String, String> out = new LinkedHashMap<>();
        if (attrs == null) {
            return out;
        }
        for (TempoTraceDto.Attribute a : attrs) {
            String v = attrValueAsString(a.getValue());
            if (v != null) {
                out.put(a.getKey(), v);
            }
        }
        return out;
    }

    private static String attrValueAsString(Map<String, Object> value) {
        if (value == null) {
            return null;
        }
        // OTLP value is a oneof; whichever key is set carries the value.
        for (Map.Entry<String, Object> e : value.entrySet()) {
            Object v = e.getValue();
            if (v == null) {
                continue;
            }
            return v.toString();
        }
        return null;
    }

    private static long parseUnixNanoToMs(String unixNano) {
        return parseUnixNano(unixNano) / 1_000_000L;
    }

    private static long parseUnixNano(String unixNano) {
        if (unixNano == null || unixNano.isBlank()) {
            return 0;
        }
        try {
            return Long.parseLong(unixNano);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
