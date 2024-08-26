package mcmp.mc.observability.agent.trigger.model.dto;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;
import mcmp.mc.observability.agent.trigger.enums.TaskStatus;

import java.util.List;

@Getter
@Setter
public class TriggerPolicyCreateDto {
    @ApiModelProperty(value = "Base64 Encoded value", example = "Y3B1IHBvbGljeQ==")
    @Base64DecodeField
    private String name;
    @ApiModelProperty(value = "Host description", example = "ZGVzY3JpcHRpb24=")
    @Base64DecodeField
    private String description;
    @ApiModelProperty(value = "Trigger target metric", example = "cpu")
    @Schema(allowableValues = {"cpu", "mem", "disk"})
    private String metric;
    @ApiModelProperty(value = "Trigger target metric field", example = "usage_idle")
    private String field;
    @ApiModelProperty(value = "Fields to group the data", example = "[\"cpu\"]")
    private List<String> groupFields;
    @ApiModelProperty(value = "Trigger target metric statistics", example = "min")
    private String statistics;
    @ApiModelProperty(value = "Agent Manager IP", example = "http://localhost:18080")
    private String agentManagerIp;
    @ApiModelProperty(value = "Base64 Encoded value",  example = "eyJjcml0IjogInZhbHVlID4gMjAiLCAid2FybiI6ICJ2YWx1ZSA+IDUwIn0=")
    @Base64DecodeField
    private String threshold;
    @ApiModelProperty(value = "Trigger Policy enablement status")
    private TaskStatus status;
}
