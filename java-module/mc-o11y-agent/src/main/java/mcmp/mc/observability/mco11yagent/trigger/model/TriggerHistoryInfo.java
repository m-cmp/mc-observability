package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64EncodeField;

@Data
@Builder
public class TriggerHistoryInfo {

    @ApiModelProperty(value = "Sequence by history", example = "1")
    @JsonProperty("seq")
    private Long seq;

    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    @JsonProperty("policy_seq")
    private Long policySeq;

    @ApiModelProperty(value = "Sequence by trigger target", example = "1")
    @JsonProperty("target_seq")
    private Long targetSeq;

    @ApiModelProperty(value = "Namespace Id")
    @JsonProperty("ns_id")
    private String nsId;

    @ApiModelProperty(value = "Vm Id")
    @JsonProperty("target_id")
    private String targetId;

    @ApiModelProperty(value = "Trigger event target metric name")
    @JsonProperty("measurement")
    private String metric;

    @ApiModelProperty(value = "Trigger event alarm details")
    @JsonProperty("data")
    private String data;

    @ApiModelProperty(value = "Trigger event level")
    @JsonProperty("level")
    private String level;

    @ApiModelProperty(value = "Base64 Encoded value", example = "localhost")
    @TriggerBase64EncodeField
    @JsonProperty("name")
    private String name;

    @ApiModelProperty(value = "The time when the history was inserted into the database", example = "2024-05-24 11:31:55")
    @JsonProperty("create_at")
    private String createAt;

    @ApiModelProperty(value = "The time when the history occurred")
    @JsonProperty("occur_time")
    private String occurTime;

}
