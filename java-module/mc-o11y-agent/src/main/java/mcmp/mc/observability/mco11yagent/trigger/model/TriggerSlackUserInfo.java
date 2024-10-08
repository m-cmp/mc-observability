package mcmp.mc.observability.mco11yagent.trigger.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64DecodeField;
import mcmp.mc.observability.mco11yagent.trigger.model.dto.TriggerSlackUserCreateDto;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class TriggerSlackUserInfo {
    @ApiModelProperty(value = "Sequence by Trigger Slack User", example = "1")
    @JsonProperty("seq")
    private Long seq;

    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    @JsonProperty("policy_seq")
    private Long policySeq;

    @ApiModelProperty(value = "Trigger Alert Slack User name")
    @JsonProperty("name")
    private String name;

    @TriggerBase64DecodeField
    @ApiModelProperty(value = "Base64 Encoded value")
    @JsonProperty("token")
    private String token;

    @TriggerBase64DecodeField
    @ApiModelProperty(value = "Base64 Encoded value")
    @JsonProperty("channel")
    private String channel;

    public void setCreateDto(TriggerSlackUserCreateDto dto) {
        this.policySeq = dto.getPolicySeq();
        this.name = dto.getName();
        this.token = dto.getToken();
        this.channel = dto.getChannel();
    }
}
