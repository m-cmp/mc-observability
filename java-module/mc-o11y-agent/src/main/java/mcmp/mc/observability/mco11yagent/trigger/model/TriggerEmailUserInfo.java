package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerEmailUserCreateDto;

@Data
public class TriggerEmailUserInfo {
    @ApiModelProperty(value = "Sequence by Trigger Email User", example = "1")
    @JsonProperty("seq")
    private Long seq;

    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    @JsonProperty("policy_seq")
    private Long policySeq;

    @ApiModelProperty(value = "Trigger Alert Email User name")
    @JsonProperty("name")
    private String name;

    @ApiModelProperty(value = "Alert Receiver Email")
    @JsonProperty("email")
    private String email;

    public void setCreatDto(TriggerEmailUserCreateDto dto) {
        this.policySeq = dto.getPolicySeq();
        this.name = dto.getName();
        this.email = dto.getEmail();
    }
}
