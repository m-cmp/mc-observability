package mcmp.mc.observability.agent.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class MeasurementFieldInfo {
    private String measurement;
    private List<FieldInfo> fields = new ArrayList<>();

    @Data
    public static class FieldInfo {
        private String fieldKey;
        private String fieldType;
    }
}
