package mcmp.mc.observability.agent.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MiningDBSetDTO {
    private String url;
    private String database;
    private String retentionPolicy;
    private String username;
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
