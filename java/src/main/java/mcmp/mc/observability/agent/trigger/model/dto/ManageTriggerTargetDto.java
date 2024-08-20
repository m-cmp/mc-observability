package mcmp.mc.observability.agent.trigger.model.dto;


import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManageTriggerTargetDto {

    @ApiModelProperty(value = "Namespace Id")
    private String nsId;
    @ApiModelProperty(value = "Vm Id")
    private String targetId;

}
