package mcmp.mc.observability.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mcmp.mc.observability.agent.enums.StateOption;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.model.dto.HostItemCreateDTO;
import mcmp.mc.observability.agent.model.dto.HostItemUpdateDTO;

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

    @JsonIgnore
    public void setCreateDto(HostItemCreateDTO dto) {
        this.hostSeq = dto.getHostSeq();
        this.pluginSeq = dto.getPluginSeq();
        this.name = dto.getName();
        this.intervalSec = dto.getIntervalSec();
        this.setting = dto.getSetting();
    }

    @JsonIgnore
    public void setUpdateDto(HostItemUpdateDTO dto) {
        this.seq = dto.getSeq();
        this.hostSeq = dto.getHostSeq();
        this.pluginSeq = dto.getPluginSeq();
        this.name = dto.getName();
        this.intervalSec = dto.getIntervalSec();
        this.setting = dto.getSetting();
    }
}