package mcmp.mc.observability.mco11ymanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class MiningDBInfo {

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
}
