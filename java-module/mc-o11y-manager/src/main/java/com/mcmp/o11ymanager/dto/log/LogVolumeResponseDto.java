package com.mcmp.o11ymanager.dto.log;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 로그 볼륨 응답 DTO
 * LogVolume 도메인 모델을 위한 응답 객체
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogVolumeResponseDto {
    private List<MetricResultDto> data;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricResultDto {
        private Map<String, String> metric;
        private List<TimeSeriesValueDto> values;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSeriesValueDto {
        private Long timestamp;
        private String value;
    }
}