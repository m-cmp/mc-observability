package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for log volume data summary response */
public class VolumeResultDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultDto {
        private List<TimeSeriesEntryDto> data;
        private StatsDto stats;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesEntryDto {
        private Long timestamp;
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
