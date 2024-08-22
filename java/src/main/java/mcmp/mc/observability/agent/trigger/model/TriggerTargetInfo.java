package mcmp.mc.observability.agent.trigger.model;


import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerTargetInfo {

    @ApiModelProperty(value = "Sequence by trigger target", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    private Long policySeq;
    @ApiModelProperty(value = "Namespace Id")
    private String nsId;
    @ApiModelProperty(value = "Vm Id")
    private String targetId;
    @ApiModelProperty(value = "Host name", example = "vm1")
    private String name;
    @ApiModelProperty(value = "Host Alias Name", example = "test-vm")
    private String aliasName;
    @ApiModelProperty(value = "The time when the trigger target was registered", example = "2024-05-24 11:31:55")
    private String createAt;
    @ApiModelProperty(value = "The time when the trigger target was updated")
    private String updateAt;

}
