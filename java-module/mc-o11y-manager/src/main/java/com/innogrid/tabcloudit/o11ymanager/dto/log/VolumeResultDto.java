package com.innogrid.tabcloudit.o11ymanager.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 로그 볼륨 데이터 요약 응답을 위한 DTO
 */
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