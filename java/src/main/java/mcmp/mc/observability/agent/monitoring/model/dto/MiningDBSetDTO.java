package mcmp.mc.observability.agent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MiningDBSetDTO {
    @ApiModelProperty(value = "Host url", example = "http://localhost:8086")
    private String url;
    @ApiModelProperty(value = "Database name", example = "mc-agent")
    private String database;
    @ApiModelProperty(value = "Data retention policy name", example = "autogen")
    private String retentionPolicy;
    @ApiModelProperty(value = "Authentication username", example = "mc-agent")
    private String username;
    @ApiModelProperty(value = "Authentication password", example = "mc-agent")
    private String password;

    @JsonIgnore
    private String oldUrl;
    @JsonIgnore
    private String oldDatabase;
    @JsonIgnore
    private String oldRetentionPolicy;
    @JsonIgnore
    private String oldUsername;
    @JsonIgnore
    private String oldPassword;
}
