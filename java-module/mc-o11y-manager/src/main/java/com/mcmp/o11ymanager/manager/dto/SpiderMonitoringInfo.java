package com.mcmp.o11ymanager.manager.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class SpiderMonitoringInfo {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {

        private String metricName;
        private String metricUnit;
        private List<TimestampValue> timestampValues;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class TimestampValue {

            private String timestamp;
            private String value;
        }
    }
}
