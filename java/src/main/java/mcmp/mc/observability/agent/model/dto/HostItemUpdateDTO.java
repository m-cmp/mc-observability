package mcmp.mc.observability.agent.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.annotation.Base64DecodeField;

@Getter
@Setter
public class HostItemUpdateDTO {
    @JsonIgnore
    private Long seq = 0L;
    @JsonIgnore
    private Long hostSeq = 0L;
    private Long pluginSeq = 0L;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64DecodeField
    private String name;
    private Integer intervalSec = 10;
    @ApiModelProperty(value = "Base64 Encoded value")
    @Base64DecodeField
    private String setting;
}
