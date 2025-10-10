package com.mcmp.o11ymanager.manager.model.log;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;


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
