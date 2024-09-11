package mcmp.mc.observability.mco11yagent.monitoring.model;

import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64DecodeField;
import mcmp.mc.observability.mco11yagent.monitoring.annotation.Base64EncodeField;

@Getter
@Setter
public class MonitoringConfigInfo {
    private Long seq;
    private String nsId;
    private String mciId;
    private String targetId;
    @Base64EncodeField
    @Base64DecodeField
    private String name;
    private String state;
    private Long pluginSeq;
    private String pluginName;
    private String pluginType;
    @Base64EncodeField
    @Base64DecodeField
    private String pluginConfig;
}
