package com.mcmp.o11ymanager.model.log;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Loki 로그 볼륨 도메인 모델
 */
@Getter
@Builder
public class LogVolume {
    private final String status;
    private final LogVolumeData data;

    @Getter
    @Builder
    public static class LogVolumeData {
        private final String resultType;
        private final List<MetricResult> result;
    }

    @Getter
    @Builder
    public static class MetricResult {
        private final Map<String, String> metric;
        private final List<TimeSeriesValue> values;
    }

    @Getter
    @Builder
    public static class TimeSeriesValue {
        private final Long timestamp;
        private final String value;
    }


}