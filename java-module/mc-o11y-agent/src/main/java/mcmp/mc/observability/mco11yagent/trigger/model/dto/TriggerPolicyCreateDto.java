package mcmp.mc.observability.mco11yagent.trigger.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.trigger.annotation.TriggerBase64DecodeField;
import mcmp.mc.observability.mco11yagent.trigger.enums.TaskStatus;

import java.util.List;

@Getter
@Setter
public class TriggerPolicyCreateDto {
    @ApiModelProperty(value = "Base64 Encoded value", example = "Y3B1IHBvbGljeQ==")
    @TriggerBase64DecodeField
    @JsonProperty("name")
    private String name;

    @ApiModelProperty(value = "Host description", example = "ZGVzY3JpcHRpb24=")
    @TriggerBase64DecodeField
    @JsonProperty("description")
    private String description;

    @ApiModelProperty(value = "Trigger target metric", example = "cpu")
    @Schema(allowableValues = {"cpu", "mem", "disk"})
    @JsonProperty("measurement")
    private String metric;

    @ApiModelProperty(value = "Trigger target metric field", example = "usage_idle")
    @JsonProperty("field")
    private String field;

	@ApiModelProperty(value = "Fields to group the data", hidden = true , example = "[]")
    @JsonProperty("group_fields")
    private List<String> groupFields;

    @ApiModelProperty(value = "Trigger target metric statistics", example = "min")
    @JsonProperty("statistics")
    private String statistics;

	@ApiModelProperty(value = "Base64 Encoded value",  example = "eyJjcml0IjogInZhbHVlID49IDEwMCIsICJ3YXJuIjogInZhbHVlID4gOTkuOSIsICJpbmZvIjogInZhbHVlIDwgOTkuNiJ9")
    @TriggerBase64DecodeField
    @JsonProperty("threshold")
    private String threshold;

    @ApiModelProperty(value = "Trigger Policy enablement status")
    @JsonProperty("status")
    private TaskStatus status;
}
