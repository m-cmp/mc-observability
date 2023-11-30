package mcmp.mc.observability.agent.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mcmp.mc.observability.agent.enums.StateOption;
import mcmp.mc.observability.agent.enums.StateYN;

@Getter
@Setter
@ToString
public class HostItemInfo {
    private Long seq = 0L;
    private Long hostSeq = 0L;
    private StateOption state;
    private StateYN monitoringYn;
    private String createAt;
    private String updateAt;
    private Long pluginSeq = 0L;
    private String pluginName;
    private String name;
    private Integer intervalSec = 10;
    private String setting;
}