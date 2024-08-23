package mcmp.mc.observability.agent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TriggerEmailUserCreateDto {

    @JsonIgnore
    private Long policySeq;
    @ApiModelProperty(value = "Trigger Alert Email User name")
    private String name;
    @ApiModelProperty(value = "Alert Receiver Email")
    private String email;
}
