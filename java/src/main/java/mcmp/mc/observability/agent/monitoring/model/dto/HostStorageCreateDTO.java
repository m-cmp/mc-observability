package mcmp.mc.observability.agent.monitoring.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.common.annotation.Base64DecodeField;

@Getter
@Setter
public class HostStorageCreateDTO {
    @JsonIgnore
    private Long hostSeq = 0L;
    @ApiModelProperty(value = "Sequence by plugin")
    private Long pluginSeq = 0L;
    @ApiModelProperty(value = "Base64 Encoded value\n" +
            "Storage name")
    @Base64DecodeField
    private String name;
    @ApiModelProperty(value = "Base64 Encoded value\n" +
            "Storage detail configuration\n" +
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
    @Base64DecodeField
    private String setting;
}
