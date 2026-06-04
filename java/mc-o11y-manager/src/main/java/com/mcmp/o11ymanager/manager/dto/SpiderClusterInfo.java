package com.mcmp.o11ymanager.manager.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Minimal projection of cb-spider cluster detail response. cb-spider uses PascalCase JSON keys
 * which Jackson does not map automatically with Lombok-generated getters whose bean-property names
 * come out lower-camel. Each field is therefore annotated with @JsonProperty so the JSON binding
 * matches the wire format exactly.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpiderClusterInfo {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class IId {
        @JsonProperty("NameId")
        private String NameId;

        @JsonProperty("SystemId")
        private String SystemId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class NodeGroup {
        @JsonProperty("IId")
        private IId IId;

        @JsonProperty("Nodes")
        private List<IId> Nodes;
    }

    @JsonProperty("IId")
    private IId IId;

    @JsonProperty("Status")
    private String Status;

    @JsonProperty("NodeGroupList")
    private List<NodeGroup> NodeGroupList;
}
