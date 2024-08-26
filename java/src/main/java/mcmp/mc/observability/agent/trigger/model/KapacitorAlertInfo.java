package mcmp.mc.observability.agent.trigger.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class KapacitorAlertInfo {
    private String id;
    private String message;
    private String details;
    private String time;
    private Long duration;
    private String level;
    private DataInfo data;
    private String previousLevel;
    private boolean recoverable;

    @Data
    public static class DataInfo {
        private List<SeriesInfo> series;

        @Data
        public static class SeriesInfo {
            private String name;
            private Map<String, Object> tags;
            private List<String> columns;
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
