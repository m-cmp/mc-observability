package com.mcmp.o11ymanager.manager.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trace responses surfaced to the UI. {@link TraceSummary} backs the list view; {@link TraceDetail}
 * is the flattened span list for a single trace (no flame graph in MVP).
 */
public class TraceResponseDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceSummary {
        @Schema(description = "Trace ID")
        private String traceId;

        @Schema(description = "Root span service name")
        private String rootService;

        @Schema(description = "Root span name")
        private String rootName;

        @Schema(description = "Trace start time (epoch ms)")
        private long startTimeMs;

        @Schema(description = "Trace duration (ms)")
        private long durationMs;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpanRow {
        @Schema(description = "Span ID")
        private String spanId;

        @Schema(description = "Parent span ID")
        private String parentSpanId;

        @Schema(description = "Service name")
        private String service;

        @Schema(description = "Span name")
        private String name;

        @Schema(description = "Span kind")
        private String kind;

        @Schema(description = "Span start time (epoch ms)")
        private long startTimeMs;

        @Schema(description = "Span duration (ms, fractional)")
        private double durationMs;

        @Schema(description = "Span attributes")
        private Map<String, String> attributes;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceDetail {
        @Schema(description = "Trace ID")
        private String traceId;

        @Schema(description = "Spans in the trace, sorted by start time")
        private List<SpanRow> spans;
    }

    /** One attribute value as Tempo typed it; the type decides how TraceQL writes the value. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeValue {
        @Schema(description = "Value as text", example = "500")
        private String value;

        @Schema(
                description =
                        "Tempo value type. TraceQL quotes string values; int, float, bool, duration"
                                + " and keyword (status, kind) values are written bare.",
                example = "int")
        private String type;
    }
}
