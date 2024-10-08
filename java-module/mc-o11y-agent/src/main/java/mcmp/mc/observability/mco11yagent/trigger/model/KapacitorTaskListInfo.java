package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class KapacitorTaskListInfo {

    @JsonProperty("tasks")
    private List<KapacitorTaskInfo> tasks;
}
