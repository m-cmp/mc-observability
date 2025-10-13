package com.mcmp.o11ymanager.manager.dto.log;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Log volume response DTO — response object for the LogVolume domain model */
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
