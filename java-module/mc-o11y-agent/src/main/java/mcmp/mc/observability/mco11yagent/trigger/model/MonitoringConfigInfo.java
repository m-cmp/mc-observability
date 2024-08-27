package mcmp.mc.observability.mco11yagent.trigger.model;


import lombok.*;
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringConfigInfo {

    private Long seq;
    private String nsId;
    private String targetId;
    private String name;
    private String state;
    private Long pluginSeq;
    private String pluginName;
    private String pluginType;
    private String config;

}
