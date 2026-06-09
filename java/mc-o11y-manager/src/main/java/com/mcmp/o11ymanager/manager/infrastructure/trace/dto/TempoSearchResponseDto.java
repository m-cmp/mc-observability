package com.mcmp.o11ymanager.manager.infrastructure.trace.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Subset of Tempo's /api/search response we render. Tempo emits more stats/metadata fields;
 * ignoring them keeps the DTO compact.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TempoSearchResponseDto {

    private List<TraceSummary> traces;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TraceSummary {
        private String traceID;
        private String rootServiceName;
        private String rootTraceName;
        private String startTimeUnixNano;
        private Long durationMs;
    }
}
