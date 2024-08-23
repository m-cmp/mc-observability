package mcmp.mc.observability.agent.trigger.model;

import lombok.Data;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerEmailUserCreateDto;

@Data
public class TriggerEmailUserInfo {
    private Long seq;
    private Long policySeq;
    private String name;
    private String email;

    public void setCreatDto(TriggerEmailUserCreateDto dto) {
        this.policySeq = dto.getPolicySeq();
        this.name = dto.getName();
        this.email = dto.getEmail();
    }
}
