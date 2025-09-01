package com.mcmp.o11ymanager.manager.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 로그 데이터 요약 응답을 위한 DTO
 */
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