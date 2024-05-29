package mcmp.mc.observability.agent.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MetricInfo {
    private final String name;
    private final List<String> columns;
    private final Map<String, String> tags;
    private final List<List<Object>> values;
}
