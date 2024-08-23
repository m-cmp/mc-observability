package mcmp.mc.observability.agent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TriggerEmailUserCreateDto {

    @JsonIgnore
    private Long policySeq;
    private String name;
    private String email;
}
