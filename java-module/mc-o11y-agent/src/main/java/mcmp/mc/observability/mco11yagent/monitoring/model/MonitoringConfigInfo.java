package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MonitoringConfigInfo {
    private Long seq;
    private String nsId;
    private String targetId;
    private String name;
    private String state;
    private Long pluginSeq;
    private String pluginName;
    private String pluginType;
    private String pluginConfig;
}
