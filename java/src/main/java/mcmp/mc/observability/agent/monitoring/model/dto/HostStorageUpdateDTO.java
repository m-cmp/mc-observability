package mcmp.mc.observability.agent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;

@Getter
@Setter
public class HostStorageUpdateDTO {
    @JsonIgnore
    private Long seq = 0L;
    @JsonIgnore
    private Long hostSeq = 0L;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64DecodeField
    private String name;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64DecodeField
    private String setting;
}
