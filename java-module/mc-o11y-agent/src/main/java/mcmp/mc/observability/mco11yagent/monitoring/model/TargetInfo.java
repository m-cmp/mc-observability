package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TargetInfo {
    private String nsId;
    private String mciId;
    private String id;
    private String name;
    private String aliasName;
    private String description;
    private String state;
}
