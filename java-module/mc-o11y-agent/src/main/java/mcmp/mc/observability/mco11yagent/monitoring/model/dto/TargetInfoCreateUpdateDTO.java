package mcmp.mc.observability.mco11yagent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64DecodeField;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;

@Getter
@Setter
public class TargetInfoCreateUpdateDTO {

    @JsonProperty("name")
    private String name;

    @Base64EncodeField
    @Base64DecodeField
    @JsonProperty("alias_name")
    private String aliasName;

    @Base64EncodeField
    @Base64DecodeField
    @JsonProperty("description")
    private String description;
}
