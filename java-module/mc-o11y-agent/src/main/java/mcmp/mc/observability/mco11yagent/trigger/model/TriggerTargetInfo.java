package mcmp.mc.observability.mco11yagent.trigger.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerTargetInfo {

    @ApiModelProperty(value = "Sequence by trigger target", example = "1")
    @JsonProperty("seq")
    private Long seq;

    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    @JsonProperty("policy_seq")
    private Long policySeq;

    @ApiModelProperty(value = "Namespace Id")
    @JsonProperty("ns_id")
    private String nsId;

    @ApiModelProperty(value = "Vm Id")
    @JsonProperty("target_id")
    private String targetId;

    @ApiModelProperty(value = "Host name", example = "vm1")
    @JsonProperty("name")
    private String name;

    @ApiModelProperty(value = "Host Alias Name", example = "test-vm")
    @JsonProperty("alias_name")
    private String aliasName;

    @ApiModelProperty(value = "The time when the trigger target was registered", example = "2024-05-24 11:31:55")
    @JsonProperty("create_at")
    private String createAt;

    @ApiModelProperty(value = "The time when the trigger target was updated")
    @JsonProperty("update_at")
    private String updateAt;
}
