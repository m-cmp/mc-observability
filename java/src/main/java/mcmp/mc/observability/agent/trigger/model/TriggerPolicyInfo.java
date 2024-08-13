package mcmp.mc.observability.agent.trigger.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;
import mcmp.mc.observability.agent.trigger.model.dto.TriggerPolicyUpdateDto;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerPolicyInfo {

    @ApiModelProperty(value = "Sequence by trigger policy", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Base64 Encoded value", example = "Y3B1IHVzYWdlX2lkbGUgY2hlY2sgcG9saWN5")
    @Base64EncodeField
    private String name;
    @ApiModelProperty(value = "Host description", example = "ZGVzY3JpcHRpb24=")
    private String description;
    @ApiModelProperty(value = "Trigger target metric", example = "cpu")
    private String metric;
//    @ApiModelProperty(value = "Base64 Encoded value",  example = "{\"crit\": \"value > 20\", \"warn\": \"value > 50\"}")
    @ApiModelProperty(value = "Base64 Encoded value",  example = "eyJjcml0IjogInZhbHVlID4gMjAiLCAid2FybiI6ICJ2YWx1ZSA+IDUwIn0=")
    @Base64EncodeField
    private Map<String,Object> threshold;
//    @JsonIgnore
//    private String tickScript;
    @ApiModelProperty(value = "Trigger Policy enablement status")
    private boolean isEnabled;
    @JsonIgnore
    private String tickScript;

    @ApiModelProperty(value = "The time when the trigger policy was registered", example = "2024-05-24 11:31:55")
    private String createAt;
    @ApiModelProperty(value = "The time when the trigger policy was updated")
    private String updateAt;

    public void setUpdateDto(TriggerPolicyUpdateDto dto) {
        this.seq = dto.getSeq();
        this.name = dto.getName();
        this.description = dto.getDescription();
        this.metric = dto.getMetric();
        this.threshold = dto.getThreshold();
        this.isEnabled = dto.isEnabled();
    }
}