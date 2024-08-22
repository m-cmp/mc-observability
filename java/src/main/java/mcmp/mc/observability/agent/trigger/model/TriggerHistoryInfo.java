package mcmp.mc.observability.agent.trigger.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;

@Data
@Builder
public class TriggerHistoryInfo {

    @ApiModelProperty(value = "Sequence by history", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    private Long policySeq;
    @ApiModelProperty(value = "Sequence by trigger target", example = "1")
    private Long targetSeq;
    @ApiModelProperty(value = "Namespace Id")
    private String nsId;
    @ApiModelProperty(value = "Vm Id")
    private String targetId;
    @ApiModelProperty(value = "Trigger event target metric name")
    private String metric;
    @ApiModelProperty(value = "Trigger event alarm details")
    private String data;
    @ApiModelProperty(value = "Trigger event level")
    private String level;
    @ApiModelProperty(value = "Base64 Encoded value", example = "localhost")
    @Base64EncodeField
    private String name;
    @ApiModelProperty(value = "The time when the history was inserted into the database", example = "2024-05-24 11:31:55")
    private String createAt;
    @ApiModelProperty(value = "The time when the history occurred")
    private String occurTime;

}
