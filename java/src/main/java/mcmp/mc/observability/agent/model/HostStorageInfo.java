package mcmp.mc.observability.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mcmp.mc.observability.agent.enums.StateOption;
import mcmp.mc.observability.agent.enums.StateYN;
import mcmp.mc.observability.agent.model.dto.HostStorageCreateDTO;
import mcmp.mc.observability.agent.model.dto.HostStorageUpdateDTO;

@Getter
@Setter
@ToString
public class HostStorageInfo {
    private Long seq = 0L;
    private Long hostSeq = 0L;
    private Long pluginSeq;
    private String pluginName;
    private String name;
    private String setting;
    private StateOption state;
    private StateYN monitoringYn;

    @JsonIgnore
    public void setCreateDto(HostStorageCreateDTO dto, PluginDefInfo plugin) {
        this.hostSeq = dto.getHostSeq();
        this.name = dto.getName();
        this.setting = dto.getSetting();
        this.pluginSeq = plugin.getSeq();
        this.pluginName = plugin.getName();
    }

    @JsonIgnore
    public void setUpdateDto(HostStorageUpdateDTO dto) {
        this.seq = dto.getSeq();
        this.hostSeq = dto.getHostSeq();
        this.name = dto.getName();
        this.setting = dto.getSetting();
    }
}