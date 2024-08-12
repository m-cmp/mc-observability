package mcmp.mc.observability.agent.monitoring.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MiningDBInfo {
    @ApiModelProperty(value = "Host url", example = "http://localhost:8086")
    private String url;
    @ApiModelProperty(value = "influxDB database name", example = "mc-agent")
    private String database;
    @ApiModelProperty(value = "influxDB retention policy", example = "autogen")
    private String retentionPolicy;
    @ApiModelProperty(value = "influxDB username", example = "mc_agent")
    private String username;
    @ApiModelProperty(value = "influxDB password", example = "mc_agent")
    private String password;
}