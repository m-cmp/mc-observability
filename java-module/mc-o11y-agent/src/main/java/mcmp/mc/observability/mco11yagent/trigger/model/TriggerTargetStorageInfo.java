package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mcmp.mc.observability.mco11yagent.monitoring.model.InfluxDBConnector;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerTargetStorageInfo {
    @ApiModelProperty(value = "Sequence by trigger target storage", example = "1")
    @JsonProperty("seq")
    private Long seq;

    @ApiModelProperty(value = "Sequence by trigger target", example = "1")
    @JsonProperty("target_seq")
    private Long targetSeq;

    @ApiModelProperty(value = "Storage url", example = "http://localhost:8086")
    @JsonProperty("url")
    private String url;

    @ApiModelProperty(value = "Storage database name", example = "mc_agent")
    @JsonProperty("database")
    private String database;

    @ApiModelProperty(value = "Storage retention policy", example = "autogen")
    @JsonProperty("retention_policy")
    private String retentionPolicy;

    @ApiModelProperty(value = "The time when the trigger target storage was registered", example = "2024-05-24T11:31:55Z")
    @JsonProperty("create_at")
    private String createAt;

    public void updateInfluxDBConfig(InfluxDBConnector influxDBConnector) {
        this.url = influxDBConnector.getUrl();
        this.database = influxDBConnector.getDatabase();
        this.retentionPolicy = influxDBConnector.getRetentionPolicy();
    }
}
