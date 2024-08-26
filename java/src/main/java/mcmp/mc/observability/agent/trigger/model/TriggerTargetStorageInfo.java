package mcmp.mc.observability.agent.trigger.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerTargetStorageInfo {
    @ApiModelProperty(value = "Sequence by trigger target storage", example = "1")
    private Long seq;
    @ApiModelProperty(value = "Sequence by trigger target", example = "1")
    private Long targetSeq;
    @ApiModelProperty(value = "Storage url", example = "http://localhost:8086")
    private String url;
    @ApiModelProperty(value = "Storage database name", example = "mc_agent")
    private String database;
    @ApiModelProperty(value = "Storage retention policy", example = "autogen")
    private String retentionPolicy;
    @ApiModelProperty(value = "The time when the trigger target storage was registered", example = "2024-05-24 11:31:55")
    private String createAt;
}
