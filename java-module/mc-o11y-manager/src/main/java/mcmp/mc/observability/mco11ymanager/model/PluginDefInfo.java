package mcmp.mc.observability.mco11ymanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema
public class PluginDefInfo {

    @JsonProperty("seq")
    @Schema(description = "Plugin identify sequence number")
    private Long seq;

    @JsonProperty("name")
    @Schema(description = "Plugin name")
    private String name;

    @JsonProperty("plugin_id")
    @Schema(description = "Plugin ID")
    private String pluginId;

    @JsonProperty("plugin_type")
    @Schema(description = "Plugin type")
    private String pluginType;
}
