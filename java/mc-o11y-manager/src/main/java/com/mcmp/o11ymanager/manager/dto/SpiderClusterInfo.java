package com.mcmp.o11ymanager.manager.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Minimal projection of cb-spider cluster detail response. Only the fields needed for CSP warming
 * (cluster name, node groups, per-group nodes) are modelled.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SpiderClusterInfo {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class IId {
        private String NameId;
        private String SystemId;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    public static class NodeGroup {
        private IId IId;
        private List<IId> Nodes;
    }

    private IId IId;
    private String Status;
    private List<NodeGroup> NodeGroupList;
}
