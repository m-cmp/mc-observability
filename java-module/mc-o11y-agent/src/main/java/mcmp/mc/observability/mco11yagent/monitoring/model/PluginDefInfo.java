package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PluginDefInfo {
    private Long seq;
    private String name;
    private String pluginId;
    private String pluginType;
}
