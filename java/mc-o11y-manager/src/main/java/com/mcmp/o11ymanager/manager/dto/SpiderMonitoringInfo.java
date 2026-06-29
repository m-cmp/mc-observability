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

        /**
         * True when cb-spider cannot provide this metric for the resource (unsupported metric, or a
         * resource — e.g. a K8s node — that has no CSP monitoring). Lets the UI show "not
         * supported" rather than a misleading "no data". Null/false on a normal response.
         */
        private Boolean unsupported;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class TimestampValue {

            private String timestamp;
            private String value;
        }
    }
}
