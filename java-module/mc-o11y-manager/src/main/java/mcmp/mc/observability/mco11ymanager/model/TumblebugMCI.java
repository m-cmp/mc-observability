package mcmp.mc.observability.mco11ymanager.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TumblebugMCI {
    @JsonProperty("id")
    private String id;

    @JsonProperty("vm")
    private Vm[] vm;

    @Getter
    @Setter
    public static class Vm {
        @JsonProperty("resource_type")
        private String resourceType;

        @JsonProperty("id")
        private String id; // example: "aws-ap-southeast-1"

        @JsonProperty("uid")
        private String uid; // example: "wef12awefadf1221edcf"

        @JsonProperty("csp_resource_name")
        private String cspResourceName; // example: "we12fawefadf1221edcf"

        @JsonProperty("csp_resource_id")
        private String cspResourceId; // example: "csp-06eb41e14121c550a"

        @JsonProperty("connection_name")
        private String connectionName;

        @JsonProperty("name")
        private String name; // example: "aws-ap-southeast-1"

        @JsonProperty("sub_group_id")
        private String subGroupId;

        @JsonProperty("description")
        private String description;

        @JsonProperty("vm_user_name")
        private String vmUserName;
    }

}
