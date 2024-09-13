package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MiningDBInfo {
    private String url;
    private String database;
    private String retentionPolicy;
    private String username;
    private String password;
}