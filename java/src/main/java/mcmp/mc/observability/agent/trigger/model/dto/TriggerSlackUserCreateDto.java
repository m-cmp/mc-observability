package mcmp.mc.observability.agent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;

@Getter
@Setter
public class TriggerSlackUserCreateDto {
    @JsonIgnore
    private Long policySeq;
    private String name;
    @Base64DecodeField
    private String token;
    @Base64DecodeField
    private String channel;
}
