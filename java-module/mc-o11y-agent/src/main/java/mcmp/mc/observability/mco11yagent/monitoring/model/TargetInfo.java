package mcmp.mc.observability.mco11yagent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64DecodeField;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;

@Getter
@Setter
public class TargetInfo {

    @JsonProperty("ns_id")
    private String nsId;

    @JsonProperty("mci_id")
    private String mciId;

    @JsonProperty("id")
    private String id;

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

    @JsonProperty("state")
    private String state;
}
