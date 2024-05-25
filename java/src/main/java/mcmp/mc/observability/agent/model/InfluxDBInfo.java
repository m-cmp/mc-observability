package mcmp.mc.observability.agent.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InfluxDBInfo {
    private String url;
    private String database;
    private String retentionPolicy;
    private String username;
    private String password;
}
