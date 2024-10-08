package mcmp.mc.observability.mco11yagent.trigger.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerMonitoringConfigInfo {

    @JsonProperty("seq")
    private Long seq;

    @JsonProperty("ns_id")
    private String nsId;

    @JsonProperty("target_id")
    private String targetId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("state")
    private String state;

    @JsonProperty("plugin_seq")
    private Long pluginSeq;

    @JsonProperty("plugin_name")
    private String pluginName;

    @JsonProperty("plugin_type")
    private String pluginType;

    @JsonProperty("plugin_config")
    private String pluginConfig;
}
