package mcmp.mc.observability.agent.model;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
public class MetricParamInfo {

    @NotBlank
    private String uuid;
    @NotBlank
    private String url;
    @NotBlank
    private String database;
    @NotBlank
    private String retentionPolicy;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String measurement;
    @NotBlank
    private String field;
    @NotBlank
    private String range; // 1s, 1m, 1h...
    @NotBlank
    private String groupTime;
    @NotBlank
    private String limit;

}
