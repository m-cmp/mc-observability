package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64DecodeField;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;

@Getter
@Setter
public class TargetInfo {
    private String nsId;
    private String mciId;
    private String id;
    private String name;
    @Base64EncodeField
    @Base64DecodeField
    private String aliasName;
    @Base64EncodeField
    @Base64DecodeField
    private String description;
    private String state;
}
