package mcmp.mc.observability.agent.monitoring.model;

import lombok.Getter;
import lombok.Setter;
import mcmp.mc.observability.agent.monitoring.enums.OS;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PluginDefInfo {
    private Long seq;
    private String name;
    private String pluginId;
    private String pluginType;
    private List<OS> os;
    private Boolean isInterval;

    public void setOs(String os) {
        if( os == null || os.isEmpty() ) return;
        if( this.os == null ) this.os = new ArrayList<>();
        for(String o : os.split(",")) {
            this.os.add(OS.parse(o));
        }
    }
}