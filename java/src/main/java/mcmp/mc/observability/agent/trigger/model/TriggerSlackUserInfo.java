package mcmp.mc.observability.agent.trigger.model;

import lombok.Data;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerSlackUserCreateDto;

@Data
public class TriggerSlackUserInfo {
    private Long seq;
    private Long policySeq;
    private String name;
    @Base64DecodeField
    private String token;
    @Base64DecodeField
    private String channel;

    public void setCreateDto(TriggerSlackUserCreateDto dto) {
        this.policySeq = dto.getPolicySeq();
        this.name = dto.getName();
        this.token = dto.getToken();
        this.channel = dto.getChannel();
    }
}
