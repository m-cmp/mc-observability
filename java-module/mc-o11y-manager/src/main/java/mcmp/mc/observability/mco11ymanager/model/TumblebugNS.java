package mcmp.mc.observability.mco11ymanager.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class TumblebugNS {
    private List<NS> ns;
    @Getter
    @Setter
    public static class NS {
        private String id;
        private String description;
        private String name;
        private String resourceType;
        private String uid;
    }
}
