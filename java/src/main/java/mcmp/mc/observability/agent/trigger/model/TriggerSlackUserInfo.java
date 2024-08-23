package mcmp.mc.observability.agent.trigger.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerSlackUserCreateDto;

@Data
public class TriggerSlackUserInfo {
    @ApiModelProperty(value = "Sequence by Trigger Slack User", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    private Long policySeq;
    @ApiModelProperty(value = "Trigger Alert Slack User name")
    private String name;
    @Base64DecodeField
    @ApiModelProperty(value = "Base64 Encoded value")
    private String token;
    @Base64DecodeField
    @ApiModelProperty(value = "Base64 Encoded value")
    private String channel;

    public void setCreateDto(TriggerSlackUserCreateDto dto) {
        this.policySeq = dto.getPolicySeq();
        this.name = dto.getName();
        this.token = dto.getToken();
        this.channel = dto.getChannel();
    }
}
