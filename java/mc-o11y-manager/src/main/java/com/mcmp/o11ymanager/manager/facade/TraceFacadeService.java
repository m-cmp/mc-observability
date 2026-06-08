package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.trace.TraceResponseDto;
import com.mcmp.o11ymanager.manager.service.interfaces.TraceService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Builds the TraceQL expression and resolves the time window for trace queries, then relays to the
 * Tempo-backed {@link TraceService}. Mirrors the log facade's role: keep query construction out of
 * the controller and the infrastructure adapter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceFacadeService {

    private static final long DEFAULT_RANGE_SEC = 60L * 60L; // last 1 hour

    private final TraceService traceService;

    public List<TraceResponseDto.TraceSummary> searchTraces(
            String query, String service, String keyword, String start, String end, int limit) {
        long[] window = resolveWindow(start, end);
        String traceQl =
                (query != null && !query.isBlank()) ? query.trim() : buildTraceQl(service, keyword);
        return traceService.searchTraces(traceQl, limit, window[0], window[1]);
    }

    public TraceResponseDto.TraceDetail getTraceDetail(String traceId) {
        return traceService.getTraceDetail(traceId);
    }

    /**
     * Builds a TraceQL selector from a service filter and a free-text keyword. An empty selector
     * ({@code { }}) matches all traces in the window.
     */
    private String buildTraceQl(String service, String keyword) {
        StringBuilder sb = new StringBuilder("{ ");
        boolean hasCondition = false;
        if (service != null && !service.isBlank()) {
            sb.append("resource.service.name=\"").append(service.trim()).append("\"");
            hasCondition = true;
        }
        if (keyword != null && !keyword.isBlank()) {
            // Escape regex specials so the user can search literal characters. (?i) for
            // case-insensitive matching against span name OR service name.
            String esc = keyword.trim().replaceAll("[\\\\.\\[\\](){}*+?^$|]", "\\\\$0");
            if (hasCondition) {
                sb.append(" && ");
            }
            sb.append("(name =~ \"(?i).*")
                    .append(esc)
                    .append(".*\" || resource.service.name =~ \"(?i).*")
                    .append(esc)
                    .append(".*\")");
        }
        sb.append(" }");
        return sb.toString();
    }

    /**
     * Resolves a [start, end] window in unix seconds. Accepts RFC3339 or unix epoch (seconds/ms)
     * strings; defaults to the last hour when both bounds are absent.
     */
    private long[] resolveWindow(String start, String end) {
        long nowSec = System.currentTimeMillis() / 1000L;
        long endSec = parseToSeconds(end, nowSec);
        long startSec = parseToSeconds(start, endSec - DEFAULT_RANGE_SEC);
        return new long[] {startSec, endSec};
    }

    private long parseToSeconds(String value, long fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String v = value.trim();
        // Unix epoch: 13 digits = ms, 10 digits = seconds.
        if (v.matches("\\d{13}")) {
            return Long.parseLong(v) / 1000L;
        }
        if (v.matches("\\d{10}")) {
            return Long.parseLong(v);
        }
        try {
            return Instant.parse(v).getEpochSecond();
        } catch (Exception e) {
            log.warn("trace window parse failed value={} -> fallback", v);
            return fallback;
        }
    }
}
