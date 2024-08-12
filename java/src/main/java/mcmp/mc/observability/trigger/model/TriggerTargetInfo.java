package mcmp.mc.observability.trigger.model;


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
    @ApiModelProperty(value = "Sequence by host", example = "1")
    private Long hostSeq;
    @ApiModelProperty(value = "Host additional configuration information (e.g., {\"key\" : \"value\"})", example = "{}")
    private String ex;
    @ApiModelProperty(value = "Base64 Encoded value", example = "localhost")
    @Base64EncodeField
    private String hostname;
    @ApiModelProperty(value = "The time when the trigger target was registered", example = "2024-05-24 11:31:55")
    private String createAt;
    @ApiModelProperty(value = "The time when the trigger target was updated")
    private String updateAt;

}
