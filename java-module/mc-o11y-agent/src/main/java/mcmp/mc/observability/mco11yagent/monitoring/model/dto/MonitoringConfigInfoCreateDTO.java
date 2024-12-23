package mcmp.mc.observability.mco11yagent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64DecodeField;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;

@Getter
@Setter
public class MonitoringConfigInfoCreateDTO {

    @Base64EncodeField
    @Base64DecodeField
    @JsonProperty("name")
    private String name;

    @JsonProperty("plugin_seq")
    private Long pluginSeq;

    @Base64EncodeField
    @Base64DecodeField
    @JsonProperty("plugin_config")
    private String pluginConfig;
}
