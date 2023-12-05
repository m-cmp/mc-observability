package mcmp.mc.observability.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mcmp.mc.observability.agent.enums.HostState;
import mcmp.mc.observability.agent.enums.OS;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.enums.TelegrafState;
import mcmp.mc.observability.agent.model.dto.HostUpdateDTO;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @JsonIgnore
    public void setUpdateHostDTO(HostUpdateDTO dto) {
        this.seq = dto.getSeq();
        this.name = dto.getName();
        this.description = dto.getDescription();
    }
}
