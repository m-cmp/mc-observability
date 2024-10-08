package mcmp.mc.observability.mco11ymanager.model;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    @JsonProperty("alias_name")
    private String aliasName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("state")
    private String state;
}
