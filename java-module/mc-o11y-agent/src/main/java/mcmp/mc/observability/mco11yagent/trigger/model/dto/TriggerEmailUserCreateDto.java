package mcmp.mc.observability.mco11yagent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TriggerEmailUserCreateDto {

    @JsonIgnore
    private Long policySeq;

    @ApiModelProperty(value = "Trigger Alert Email User name")
    @JsonProperty("name")
    private String name;

    @ApiModelProperty(value = "Alert Receiver Email")
    @JsonProperty("email")
    private String email;
}
