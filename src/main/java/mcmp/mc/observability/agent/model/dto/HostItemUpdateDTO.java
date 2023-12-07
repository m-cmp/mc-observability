package mcmp.mc.observability.agent.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostItemUpdateDTO {
    @JsonIgnore
    private Long seq = 0L;
    @JsonIgnore
    private Long hostSeq = 0L;
    private Long pluginSeq = 0L;
    private String name;
    private Integer intervalSec = 10;
    private String setting;
}
