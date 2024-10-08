package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SpiderMonitoringInfo {

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Data {

        @JsonProperty("metric_name")
        private String metricName;

        @JsonProperty("metric_unit")
        private String metricUnit;

        @JsonProperty("timestamp_values")
        private List<TimestampValue> timestampValues;

        @Getter
        @Setter
        @NoArgsConstructor
        public static class TimestampValue {

            @JsonProperty("timestamp")
            private String timestamp;

            @JsonProperty("value")
            private String value;
        }
    }
}
