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
    @JsonProperty("old_url")
    private String oldUrl;

    @JsonIgnore
    @JsonProperty("old_database")
    private String oldDatabase;

    @JsonIgnore
    @JsonProperty("old_retention_policy")
    private String oldRetentionPolicy;

    @JsonIgnore
    @JsonProperty("old_username")
    private String oldUsername;

    @JsonIgnore
    @JsonProperty("old_password")
    private String oldPassword;
}
