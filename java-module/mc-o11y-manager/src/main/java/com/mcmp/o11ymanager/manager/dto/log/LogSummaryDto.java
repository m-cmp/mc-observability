package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class LogSummaryDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultDto {
        private String status;
        private List<LogEntryDto> data;
        private StatsDto stats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LogEntryDto {
        private Map<String, String> labels;
        private Double timestamp;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsDto {
        private Long totalBytesProcessed;
        private Long totalLinesProcessed;
        private Double execTime;
        private Integer totalEntriesReturned;
    }
}
