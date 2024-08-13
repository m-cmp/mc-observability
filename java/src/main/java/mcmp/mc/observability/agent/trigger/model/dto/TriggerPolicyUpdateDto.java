package mcmp.mc.observability.agent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;

import java.util.Map;

@Getter
@Setter
public class TriggerPolicyUpdateDto {
    @JsonIgnore
    private Long seq = 0L;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64DecodeField
    private String name;
    @ApiModelProperty(value = "Host description", example = "ZGVzY3JpcHRpb24=")
    @Base64DecodeField
    private String description;
    @ApiModelProperty(value = "Trigger target metric", example = "cpu")
    private String metric;
//    @ApiModelProperty(value = "Base64 Encoded value",  example = "{\"crit\": \"value > 20\", \"warn\": \"value > 50\"}")
    @ApiModelProperty(value = "Base64 Encoded value",  example = "eyJjcml0IjogInZhbHVlID4gMjAiLCAid2FybiI6ICJ2YWx1ZSA+IDUwIn0=")
    @Base64EncodeField
    private Map<String,Object> threshold;
    @ApiModelProperty(value = "Trigger Policy enablement status")
    private boolean isEnabled;
}
