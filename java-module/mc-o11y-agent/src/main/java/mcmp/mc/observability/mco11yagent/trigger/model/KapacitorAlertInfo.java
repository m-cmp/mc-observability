package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class KapacitorAlertInfo {
    @JsonProperty("id")
    private String id;

    @JsonProperty("message")
    private String message;

    @JsonProperty("details")
    private String details;

    @JsonProperty("time")
    private String time;

    @JsonProperty("duration")
    private Long duration;

    @JsonProperty("level")
    private String level;

    @JsonProperty("data")
    private DataInfo data;

    private String previousLevel;

    @JsonProperty("recoverable")
    private boolean recoverable;

    @Data
    public static class DataInfo {
        @JsonProperty("series")
        private List<SeriesInfo> series;

        @Data
        public static class SeriesInfo {
            @JsonProperty("name")
            private String name;

            @JsonProperty("tags")
            private Map<String, Object> tags;

            @JsonProperty("columns")
            private List<String> columns;

            @JsonProperty("values")
            private List<Object> values;

            @Override
            public String toString() {
                return "name=" + name +
                        ", tags=" + tags +
                        ", columns=" + columns +
                        ", values=" + values;
            }
        }
    }
}
