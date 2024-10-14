package mcmp.mc.observability.mco11ymanager.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11ymanager.annotation.Base64DecodeField;
import mcmp.mc.observability.mco11ymanager.annotation.Base64EncodeField;

@Getter
@Setter
public class MonitoringConfigInfoUpdateDTO {

    @JsonProperty(value = "seq")
    private Long seq;

    @JsonProperty("ns_id")
    private String nsId;

    @JsonProperty("mci_id")
    private String mciId;

    @JsonProperty("target_id")
    private String targetId;

    @Base64EncodeField
    @Base64DecodeField
    @JsonProperty("name")
    private String name;

    @JsonProperty("plugin_seq")
    private Long pluginSeq;

    @JsonProperty("plugin_name")
    private String pluginName;

    @JsonProperty("plugin_type")
    private String pluginType;

    @Base64EncodeField
    @Base64DecodeField
    @JsonProperty("plugin_config")
    private String pluginConfig;
}
