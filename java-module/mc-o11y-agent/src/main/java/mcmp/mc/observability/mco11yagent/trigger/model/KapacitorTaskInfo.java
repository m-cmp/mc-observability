package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class KapacitorTaskInfo {
    @JsonProperty("id")
    private String id;

    @JsonProperty("type")
    private String type;

    @JsonProperty("dbrps")
    private List<Map<String, String>> dbrps;

    @JsonProperty("script")
    private String script;

    @JsonProperty("status")
    private String status;
}
