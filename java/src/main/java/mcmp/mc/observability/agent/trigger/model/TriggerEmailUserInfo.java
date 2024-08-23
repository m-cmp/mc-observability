package mcmp.mc.observability.agent.trigger.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerEmailUserCreateDto;

@Data
public class TriggerEmailUserInfo {
    @ApiModelProperty(value = "Sequence by Trigger Email User", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    private Long policySeq;
    @ApiModelProperty(value = "Trigger Alert Email User name")
    private String name;
    @ApiModelProperty(value = "Alert Receiver Email")
    private String email;

    public void setCreatDto(TriggerEmailUserCreateDto dto) {
        this.policySeq = dto.getPolicySeq();
        this.name = dto.getName();
        this.email = dto.getEmail();
    }
}
