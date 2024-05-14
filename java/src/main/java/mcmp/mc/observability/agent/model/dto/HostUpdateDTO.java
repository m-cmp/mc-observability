package mcmp.mc.observability.agent.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HostUpdateDTO {
    @JsonIgnore
    private Long seq = 0L;
    private String name;
    private String description;
}
