package com.mcmp.o11ymanager.manager.infrastructure.trace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tempo's /api/traces/{id} returns the OTLP wire shape (batches / resourceSpans / scopeSpans /
 * spans). Mirrored loosely as nested DTOs because the trace detail is flattened into a per-span
 * table on the way out.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TempoTraceDto {

    private List<Batch> batches;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Batch {
        private Resource resource;
        private List<ScopeSpans> scopeSpans;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Resource {
        private List<Attribute> attributes;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScopeSpans {
        private Map<String, Object> scope;
        private List<Span> spans;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Span {
        private String traceId;
        private String spanId;
        private String parentSpanId;
        private String name;
        private String kind;
        private String startTimeUnixNano;
        private String endTimeUnixNano;
        private List<Attribute> attributes;
        private Map<String, Object> status;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Attribute {
        private String key;
        private Map<String, Object> value;
    }
}
