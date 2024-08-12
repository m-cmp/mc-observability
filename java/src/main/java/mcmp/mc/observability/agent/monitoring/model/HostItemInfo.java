package mcmp.mc.observability.agent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;
import mcmp.mc.observability.agent.monitoring.enums.StateOption;
import mcmp.mc.observability.agent.monitoring.enums.StateYN;
import mcmp.mc.observability.agent.monitoring.model.dto.HostItemCreateDTO;
import mcmp.mc.observability.agent.monitoring.model.dto.HostItemUpdateDTO;

@Getter
@Setter
@ToString
public class HostItemInfo {
    @ApiModelProperty(value = "Sequence by monitoring item")
    private Long seq = 0L;
    @ApiModelProperty(value = "Sequence by host")
    private Long hostSeq = 0L;
    private StateOption state;
    @ApiModelProperty(value = "Status of monitoring activation (e.g., \"Y\", \"N\")")
    private StateYN monitoringYn;
    @ApiModelProperty(value = "The time when the item was registered")
    private String createAt;
    @ApiModelProperty(value = "The time when the item was updated")
    private String updateAt;
    @ApiModelProperty(value = "Sequence by item plugin")
    private Long pluginSeq = 0L;
    @ApiModelProperty(value = "Plugin name of item")
    private String pluginName;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64EncodeField
    private String name;
    @ApiModelProperty(value = "Item interval second")
    private Integer intervalSec = 10;
    @ApiModelProperty(value = "Item interval setting")
    private Boolean isInterval;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64EncodeField
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