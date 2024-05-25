package mcmp.mc.observability.agent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Builder
public class InfluxDBInfo {
    private Long seq;
    private String url;
    private String database;
    private String retentionPolicy;
    private String username;
    private String password;

    @Override
    public int hashCode() {
        return Objects.hash(url, database, retentionPolicy, username, password);
    }
}
