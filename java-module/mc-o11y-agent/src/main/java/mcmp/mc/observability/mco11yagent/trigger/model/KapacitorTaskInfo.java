package mcmp.mc.observability.mco11yagent.trigger.model;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class KapacitorTaskInfo {

    private String id;
    private String type;
    private List<Map<String, String>> dbrps;
    private String script;
    private String status;

}