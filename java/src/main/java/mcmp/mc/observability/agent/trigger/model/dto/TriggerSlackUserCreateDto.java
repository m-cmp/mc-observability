package mcmp.mc.observability.agent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;

@Getter
@Setter
public class TriggerSlackUserCreateDto {
    @JsonIgnore
    private Long policySeq;
    @ApiModelProperty(value = "Trigger Alert Slack User name")
    private String name;
    @Base64DecodeField
    @ApiModelProperty(value = "Base64 Encoded value")
    private String token;
    @Base64DecodeField
    @ApiModelProperty(value = "Base64 Encoded value")
    private String channel;
}
