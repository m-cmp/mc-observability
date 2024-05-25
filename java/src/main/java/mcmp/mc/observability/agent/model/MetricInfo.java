package mcmp.mc.observability.agent.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class MetricInfo {
    private final String name;
    private final List<String> columns;
    private final Map<String, String> tags;
    private final List<List<Object>> values;

    public MetricInfo(String name, List<String> columns, Map<String, String> tags, List<List<Object>> values) {
        this.name = name;
        this.columns = columns;
        this.tags = tags;
        this.values = values;
    }
}
