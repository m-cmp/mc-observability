package mcmp.mc.observability.mco11yagent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MiningDBSetDTO {

    @JsonProperty("url")
    private String url;

    @JsonProperty("database")
    private String database;

    @JsonProperty("retention_policy")
    private String retentionPolicy;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
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
