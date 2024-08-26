package mcmp.mc.observability.agent.monitoring.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import mcmp.mc.observability.agent.common.annotation.Base64EncodeField;
import mcmp.mc.observability.agent.monitoring.enums.StateOption;
import mcmp.mc.observability.agent.monitoring.enums.StateYN;
import mcmp.mc.observability.agent.monitoring.model.dto.HostStorageCreateDTO;
import mcmp.mc.observability.agent.monitoring.model.dto.HostStorageUpdateDTO;

@Getter
@Setter
@ToString
public class HostStorageInfo {
    @ApiModelProperty(value = "Sequence by storage")
    private Long seq = 0L;
    @ApiModelProperty(value = "Sequence by host")
    private Long hostSeq = 0L;
    @ApiModelProperty(value = "Sequence by item plugin")
    private Long pluginSeq;
    @ApiModelProperty(value = "Plugin name of item")
    private String pluginName;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64EncodeField
    private String name;
    @ApiModelProperty(value =   "Base64 encoded json string\n" +
            "for example influxdb plugin)\n" +
            "&nbsp;&nbsp;\"urls\": [\"http://localhost:8086\"] //(* Require)\n" +
            "&nbsp;&nbsp;\"database\": \"m-cmp\" //(* Require)\n" +
            "&nbsp;&nbsp;\"retention_policy\": \"autogen\" //(* Require)\n" +
            "&nbsp;&nbsp;\"username\": \"admin\" //(* Require)\n" +
            "&nbsp;&nbsp;\"password\": \"admin\" //(* Require) \n\n" +
            "for example elasticsearch plugin)\n" +
            "&nbsp;&nbsp;\"urls\": [\"http://node1.es.example.com:9200\"] //(* Require)\n" +
            "&nbsp;&nbsp;\"index_name \": \"telegraf-%Y.%m.%d\" //(* Require)"
    )
    @Base64EncodeField
    private String setting;
    private StateOption state;
    @ApiModelProperty(value = "Status of monitoring activation (e.g., \"Y\", \"N\")")
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