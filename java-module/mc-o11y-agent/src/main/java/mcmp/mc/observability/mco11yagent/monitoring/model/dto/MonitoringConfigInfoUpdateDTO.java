package mcmp.mc.observability.mco11yagent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64DecodeField;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;

@Getter
@Setter
public class MonitoringConfigInfoUpdateDTO {

    @JsonProperty(value = "seq")
    private Long seq;

    @Base64EncodeField
    @Base64DecodeField
    @JsonProperty("plugin_config")
    private String pluginConfig;
}
