package mcmp.mc.observability.agent.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mcmp.mc.observability.agent.annotation.Base64EncodeField;
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
    @ApiModelProperty(value = "Sequence by host")
    private Long seq = 0L;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64EncodeField
    private String name;
    @ApiModelProperty(value = "Uuid by host")
    private String uuid;
    @ApiModelProperty(value = "Kind of host (e.g., \"LINUX\", \"WINDOWS\")")
    private OS os;
    @ApiModelProperty(value = "Status of monitoring activation (e.g., \"Y\", \"N\")")
    private StateYN monitoringYn;
    @ApiModelProperty(value = "Agent running status (e.g., \"ACTIVE\", \"INACTIVE\")")
    private HostState state;
    @ApiModelProperty(value = "telegraf running status (e.g., \"RUNNING\", \"STOPPED\", \"FAILED\")")
    private TelegrafState telegrafState;
    @ApiModelProperty(value = "The time when the host was registered")
    private String createAt;
    @ApiModelProperty(value = "The time when the host was updated")
    private String updateAt;
    @ApiModelProperty(value = "Host additional configuration information (e.g., {\"key\" : \"value\"})")
    private String ex;
    @ApiModelProperty(value = "Host description")
    private String description;
    @ApiModelProperty(value = "Host sync status (e.g., \"Y\", \"N\")")
    private StateYN syncYN;
    @ApiModelProperty(value = "Number of items registered to the host")
    private Long itemCount = 0L;
    @ApiModelProperty(value = "Number of storages registered to the host")
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
