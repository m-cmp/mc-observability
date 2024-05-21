package mcmp.mc.observability.agent.model.dto;

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
}
