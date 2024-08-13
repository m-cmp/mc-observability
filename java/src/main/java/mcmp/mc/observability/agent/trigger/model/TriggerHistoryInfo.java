package mcmp.mc.observability.agent.trigger.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;

@Data
public class TriggerHistoryInfo {

    @ApiModelProperty(value = "Sequence by history", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    private Long policySeq;
    @ApiModelProperty(value = "Sequence by trigger target", example = "1")
    private Long targetSeq;
    @ApiModelProperty(value = "Uuid by host", example = "07250583-a9ef-c636-6113-e80289a64ce6")
    private String agentUuid;
    @ApiModelProperty(value = "Trigger event target metric name")
    private String metricName;
    @ApiModelProperty(value = "Trigger event alarm details")
    private String data;
    @ApiModelProperty(value = "Trigger event level")
    private String level;
    @ApiModelProperty(value = "Base64 Encoded value", example = "localhost")
    @Base64EncodeField
    private String hostname; // cmpAgent Hostname
    @ApiModelProperty(value = "Host additional configuration information (e.g., {\"key\" : \"value\"})", example = "{}")
    private String ex; // cmpAgent ex
    @ApiModelProperty(value = "The time when the history was inserted into the database", example = "2024-05-24 11:31:55")
    private String createAt;
    @ApiModelProperty(value = "The time when the history occurred")
    private String occurTime;

}
