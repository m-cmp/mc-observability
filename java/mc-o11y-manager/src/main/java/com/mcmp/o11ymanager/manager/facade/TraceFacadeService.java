package com.mcmp.o11ymanager.manager.facade;

import com.mcmp.o11ymanager.manager.dto.trace.TraceResponseDto;
import com.mcmp.o11ymanager.manager.service.interfaces.TraceService;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Builds the TraceQL expression and resolves the time window for trace queries, then relays to the
 * Tempo-backed {@link TraceService}. Mirrors the log facade's role: keep query construction out of
 * the controller and the infrastructure adapter.
 *
 * <p>{@code scope} splits the trace store into "framework" (the o11y platform's own services, whose
 * service.name starts with {@code framework-service-prefix}) and "vm" (everything else — the target
 * VMs' Beyla/OTel application traces), so the two are never mixed in one list.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TraceFacadeService {

    private static final long DEFAULT_RANGE_SEC = 60L * 60L; // last 1 hour

    public static final String SCOPE_FRAMEWORK = "framework";
    public static final String SCOPE_VM = "vm";

    private final TraceService traceService;

    @Value("${tempo.framework-service-prefix:mc-observability}")
    private String frameworkServicePrefix;

    public List<TraceResponseDto.TraceSummary> searchTraces(
            String query,
            String service,
            String keyword,
            String scope,
            String start,
            String end,
            int limit) {
        long[] window = resolveWindow(start, end);
        String traceQl =
                (query != null && !query.isBlank())
                        ? query.trim()
                        : buildTraceQl(service, keyword, scope);
        return traceService.searchTraces(traceQl, limit, window[0], window[1]);
    }

    public TraceResponseDto.TraceDetail getTraceDetail(String traceId) {
        return traceService.getTraceDetail(traceId);
    }

    /** Service names known to Tempo, optionally narrowed to a scope (framework / vm). */
    public List<String> getServiceNames(String scope) {
        return traceService.getServiceNames().stream()
                .filter(s -> s != null && !s.isBlank())
                .filter(s -> matchesScope(s, scope))
                .sorted()
                .collect(Collectors.toList());
    }

    private boolean isFrameworkService(String serviceName) {
        return serviceName.startsWith(frameworkServicePrefix);
    }

    private boolean matchesScope(String serviceName, String scope) {
        if (SCOPE_FRAMEWORK.equalsIgnoreCase(scope)) {
            return isFrameworkService(serviceName);
        }
        if (SCOPE_VM.equalsIgnoreCase(scope)) {
            return !isFrameworkService(serviceName);
        }
        return true; // null/all
    }

    /**
     * Builds a TraceQL selector from a service filter, scope, and a free-text keyword. An empty
     * selector ({@code { }}) matches all traces in the window.
     */
    private String buildTraceQl(String service, String keyword, String scope) {
        StringBuilder sb = new StringBuilder("{ ");
        boolean hasCondition = false;
        if (service != null && !service.isBlank()) {
            sb.append("resource.service.name=\"").append(service.trim()).append("\"");
            hasCondition = true;
        } else if (SCOPE_FRAMEWORK.equalsIgnoreCase(scope)) {
            sb.append("resource.service.name=~\"").append(frameworkServicePrefix).append(".*\"");
            hasCondition = true;
        } else if (SCOPE_VM.equalsIgnoreCase(scope)) {
            sb.append("resource.service.name!~\"").append(frameworkServicePrefix).append(".*\"");
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
