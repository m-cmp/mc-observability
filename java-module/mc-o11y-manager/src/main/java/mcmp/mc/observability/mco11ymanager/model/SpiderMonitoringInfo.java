package mcmp.mc.observability.mco11ymanager.model;

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
