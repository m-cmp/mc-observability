package mcmp.mc.observability.mco11yagent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManageTriggerTargetDto {

    @ApiModelProperty(value = "Namespace Id")
    @JsonProperty("ns_id")
    private String nsId;

    @ApiModelProperty(value = "Vm Id")
    @JsonProperty("target_id")
    private String targetId;
}
