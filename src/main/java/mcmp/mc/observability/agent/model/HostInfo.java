package mcmp.mc.observability.agent.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.enums.HostState;
import mcmp.mc.observability.agent.enums.OS;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.enums.TelegrafState;

import java.util.Map;

@Getter
@Setter
@Builder
public class HostInfo {
    private Long seq = 0L;
    private String name;
    private String uuid;
    private OS os;
    private StateYN monitoringYn;
    private HostState state;
    private TelegrafState telegrafState;
    private String createAt;
    private String updateAt;
    private String ex;
    private String description;
    private StateYN syncYN;
    private Long itemCount = 0L;
    private Long storageCount = 0L;

    public void mappingCount(Map<Long, Long> itemMap, Map<Long, Long> storageMap) {
        if( itemMap != null )    this.setItemCount(   (itemMap.get(seq) == null?    0: itemMap.get(seq)));
        if( storageMap != null ) this.setStorageCount((storageMap.get(seq) == null? 0: storageMap.get(seq)));
    }
}
