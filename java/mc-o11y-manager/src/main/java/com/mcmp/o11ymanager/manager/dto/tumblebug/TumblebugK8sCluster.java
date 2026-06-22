package com.mcmp.o11ymanager.manager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/** Minimal view of a cb-tumblebug K8sCluster (only the fields the agent install needs). */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class TumblebugK8sCluster {
    private String id;
    private String name;
    private String connectionName;
    private String status;
    private AccessInfo accessInfo;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AccessInfo {
        private String endpoint;
        private String kubeconfig;
    }

    /** Wrapper for the list endpoint response: {"K8sClusterInfo":[...]}. */
    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListResponse {
        @JsonProperty("K8sClusterInfo")
        private List<TumblebugK8sCluster> k8sClusterInfo;
    }
}
