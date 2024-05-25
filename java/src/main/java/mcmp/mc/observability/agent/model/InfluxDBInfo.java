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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InfluxDBInfo that = (InfluxDBInfo) o;

        if (!Objects.equals(url, that.url)) return false;
        if (!Objects.equals(database, that.database)) return false;
        if (!Objects.equals(retentionPolicy, that.retentionPolicy)) return false;
        if (!Objects.equals(username, that.username)) return false;
        return Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, database, retentionPolicy, username, password);
    }
}
