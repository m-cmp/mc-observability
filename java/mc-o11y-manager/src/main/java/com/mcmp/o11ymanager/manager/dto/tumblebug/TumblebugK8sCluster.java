package com.mcmp.o11ymanager.manager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import java.util.List;
import java.util.Map;
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

    /**
     * The cb-spider cluster detail cb-tumblebug fetched for this cluster (node groups and their
     * nodes), so callers need not ask cb-spider again. May be absent on older cb-tumblebug.
     */
    private SpiderClusterInfo spiderViewK8sClusterDetail;

    /** cb-tumblebug system labels; carries {@code sys.cspResourceName} among others. */
    private Map<String, String> label;

    /**
     * CSP-native cluster resource name (e.g. the Azure AKS managed-cluster name), used as
     * cb-spider's {@code clusterName} path variable. cb-tumblebug stores it under the {@code
     * sys.cspResourceName} label.
     */
    public String getCspResourceName() {
        return label == null ? null : label.get("sys.cspResourceName");
    }

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
