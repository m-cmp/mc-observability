package mcmp.mc.observability.agent.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mcmp.mc.observability.agent.enums.StateOption;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.enums.StorageKind;

@Getter
@Setter
@ToString
public class HostStorageInfo {
    private Long seq = 0L;
    private Long hostSeq = 0L;
    private StorageKind kind;
    private String name;
    private String info;
    private StateOption state;
    private StateYN monitoringYn;
}