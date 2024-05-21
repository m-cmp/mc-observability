package mcmp.mc.observability.agent.model;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningDBInfo {

    private Long seq = 0L;
    private String url;
    private String database;
    private String retentionPolicy;
    private String username;
    private String password;

}
