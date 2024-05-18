package mcmp.mc.observability.agent.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostStorageCreateDTO {
    @JsonIgnore
    private Long hostSeq = 0L;
    private Long pluginSeq = 0L;
    private String name;
    private String setting;
}
