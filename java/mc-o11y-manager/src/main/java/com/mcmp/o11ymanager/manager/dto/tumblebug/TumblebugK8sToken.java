package com.mcmp.o11ymanager.manager.dto.tumblebug;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * cb-tumblebug {@code /k8sCluster/{id}/token} response. cb-tumblebug wraps a Kubernetes client-go
 * {@code ExecCredential} under an {@code execCredential} key; we only need the bearer token from
 * its status to authenticate fabric8 against exec-plugin clusters (AWS EKS, etc.).
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TumblebugK8sToken {

    @JsonProperty("execCredential")
    private ExecCredential execCredential;

    /** Convenience accessor for the bearer token, or {@code null} when absent. */
    public String getToken() {
        if (execCredential == null || execCredential.getStatus() == null) {
            return null;
        }
        return execCredential.getStatus().getToken();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExecCredential {
        private Status status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Status {
        private String token;
    }
}
