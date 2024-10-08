package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
@Builder
@ApiModel(description = "InfluxDB information")
public class InfluxDBInfo {
    @ApiModelProperty(value = "Sequence number")
    @JsonProperty("seq")
    private Long seq;

    @ApiModelProperty(value = "Host url", example = "http://localhost:8086")
    @JsonProperty("url")
    private String url;

    @ApiModelProperty(value = "Database name", example = "mc-agent")
    @JsonProperty("database")
    private String database;

    @ApiModelProperty(value = "Data retention policy name", example = "autogen")
    @JsonProperty("retention_policy")
    private String retentionPolicy;

    @ApiModelProperty(value = "Authentication username", example = "mc-agent")
    @JsonProperty("username")
    private String username;

    @ApiModelProperty(value = "Authentication password", example = "mc-agent")
    @JsonProperty("password")
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
