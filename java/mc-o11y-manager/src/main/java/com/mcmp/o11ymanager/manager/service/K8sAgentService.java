package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugK8sCluster;
import com.mcmp.o11ymanager.manager.facade.InfluxDbFacadeService;
import com.mcmp.o11ymanager.manager.infrastructure.tumblebug.TumblebugClient;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Installs/uninstalls a host-level Telegraf agent on every node of a Kubernetes cluster, using only
 * the cluster kubeconfig (from cb-tumblebug). A short-lived privileged Job per node enters the host
 * namespaces via {@code nsenter} and installs Telegraf as a systemd service on the node host (not a
 * sidecar container). Metrics are shipped to InfluxDB with the same tag schema as VM agents ({@code
 * ns_id} / {@code infra_id}=clusterId / {@code node_id}=k8s node name).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class K8sAgentService {

    private final TumblebugClient tumblebugClient;
    private final InfluxDbFacadeService influxDbFacadeService;

    private static final String JOB_NS = "default";
    private static final String TELEGRAF_VERSION = "1.29.5";
    private static final String NSENTER_IMAGE = "alpine:3.20";

    /** Node considered "running" if it reported within this window. */
    private static final long FRESH_SECONDS = 180;

    /** Host telegraf input plugins selectable per node, with their measurement name. */
    public static final java.util.LinkedHashMap<String, String> INPUTS =
            new java.util.LinkedHashMap<>();

    static {
        INPUTS.put("cpu", "[[inputs.cpu]]\n  totalcpu = true\n  percpu = false");
        INPUTS.put("mem", "[[inputs.mem]]");
        INPUTS.put("disk", "[[inputs.disk]]");
        INPUTS.put("diskio", "[[inputs.diskio]]");
        INPUTS.put("net", "[[inputs.net]]");
        INPUTS.put("system", "[[inputs.system]]");
        INPUTS.put("processes", "[[inputs.processes]]");
        INPUTS.put("swap", "[[inputs.swap]]");
    }

    private static final List<String> DEFAULT_METRICS = new ArrayList<>(INPUTS.keySet());

    /** Log agent (fluent-bit) runs as a podman container on the node host, shipping to Loki. */
    private static final String FLUENT_BIT_IMAGE = "docker.io/fluent/fluent-bit:3.2.4";

    private static final int LOKI_PORT = 3100;

    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Getter
    @lombok.AllArgsConstructor
    public static class NodeResult {
        private String node;
        private boolean ok;
        private String message;
    }

    @Getter
    @lombok.AllArgsConstructor
    public static class NodeStatus {
        private String node;
        private boolean installed; // job-created marker / telegraf present
        private boolean running; // reported to InfluxDB recently
        private String lastSeen; // ISO timestamp or null
    }

    private KubernetesClient client(String nsId, String clusterId) {
        TumblebugK8sCluster cluster = tumblebugClient.getK8sCluster(nsId, clusterId);
        if (cluster == null
                || cluster.getAccessInfo() == null
                || cluster.getAccessInfo().getKubeconfig() == null
                || cluster.getAccessInfo().getKubeconfig().isBlank()) {
            throw new IllegalStateException(
                    "kubeconfig not available for cluster " + nsId + "/" + clusterId);
        }
        Config cfg = Config.fromKubeconfig(cluster.getAccessInfo().getKubeconfig());
        return new KubernetesClientBuilder().withConfig(cfg).build();
    }

    public List<NodeResult> install(String nsId, String clusterId) {
        return install(nsId, clusterId, DEFAULT_METRICS);
    }

    public List<NodeResult> install(String nsId, String clusterId, List<String> metrics) {
        InfluxDTO influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
        List<NodeResult> results = new ArrayList<>();
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            for (Node node : k8s.nodes().list().getItems()) {
                results.add(
                        installOne(
                                k8s,
                                nsId,
                                clusterId,
                                node.getMetadata().getName(),
                                influx,
                                metrics));
            }
        }
        return results;
    }

    public NodeResult installNode(
            String nsId, String clusterId, String nodeName, List<String> metrics) {
        InfluxDTO influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            return installOne(k8s, nsId, clusterId, nodeName, influx, metrics);
        }
    }

    public List<NodeResult> uninstall(String nsId, String clusterId) {
        List<NodeResult> results = new ArrayList<>();
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            for (Node node : k8s.nodes().list().getItems()) {
                results.add(uninstallOne(k8s, node.getMetadata().getName()));
            }
        }
        return results;
    }

    public NodeResult uninstallNode(String nsId, String clusterId, String nodeName) {
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            return uninstallOne(k8s, nodeName);
        }
    }

    private NodeResult installOne(
            KubernetesClient k8s,
            String nsId,
            String clusterId,
            String nodeName,
            InfluxDTO influx,
            List<String> metrics) {
        try {
            String script = installScript(nsId, clusterId, nodeName, influx, metrics);
            runJob(k8s, "cmp-telegraf-install-", nodeName, script);
            return new NodeResult(nodeName, true, "installed");
        } catch (Exception e) {
            log.error("k8s agent install failed node={}", nodeName, e);
            return new NodeResult(nodeName, false, e.getMessage());
        }
    }

    private NodeResult uninstallOne(KubernetesClient k8s, String nodeName) {
        try {
            runJob(k8s, "cmp-telegraf-uninstall-", nodeName, uninstallScript());
            return new NodeResult(nodeName, true, "uninstalled");
        } catch (Exception e) {
            log.error("k8s agent uninstall failed node={}", nodeName, e);
            return new NodeResult(nodeName, false, e.getMessage());
        }
    }

    /** Input plugin names currently producing data for the node (recent measurements). */
    public List<String> nodeActiveMetrics(String nsId, String clusterId, String nodeName) {
        InfluxDTO influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
        List<String> active = new ArrayList<>();
        for (String m : INPUTS.keySet()) {
            try {
                String q =
                        "SELECT last(*) FROM "
                                + m
                                + " WHERE infra_id='"
                                + clusterId
                                + "' AND node_id='"
                                + nodeName
                                + "'";
                if (influxHasSeries(influx, q)) {
                    active.add(m);
                }
            } catch (Exception ignore) {
                // skip
            }
        }
        return active;
    }

    // --- Log agent (fluent-bit via podman) -------------------------------

    public List<NodeResult> installLog(String nsId, String clusterId) {
        String lokiHost = lokiHost(nsId, clusterId);
        List<NodeResult> results = new ArrayList<>();
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            for (Node node : k8s.nodes().list().getItems()) {
                results.add(
                        installLogOne(
                                k8s, nsId, clusterId, node.getMetadata().getName(), lokiHost));
            }
        }
        return results;
    }

    public NodeResult installLogNode(String nsId, String clusterId, String nodeName) {
        String lokiHost = lokiHost(nsId, clusterId);
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            return installLogOne(k8s, nsId, clusterId, nodeName, lokiHost);
        }
    }

    public List<NodeResult> uninstallLog(String nsId, String clusterId) {
        List<NodeResult> results = new ArrayList<>();
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            for (Node node : k8s.nodes().list().getItems()) {
                results.add(uninstallLogOne(k8s, node.getMetadata().getName()));
            }
        }
        return results;
    }

    public NodeResult uninstallLogNode(String nsId, String clusterId, String nodeName) {
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            return uninstallLogOne(k8s, nodeName);
        }
    }

    /** Per-node log agent status (running = node logs present in Loki recently). */
    public List<NodeStatus> logStatus(String nsId, String clusterId) {
        String lokiHost = lokiHost(nsId, clusterId);
        List<NodeStatus> out = new ArrayList<>();
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            for (Node node : k8s.nodes().list().getItems()) {
                String nodeName = node.getMetadata().getName();
                boolean running = lokiHasRecent(lokiHost, nsId, clusterId, nodeName);
                out.add(new NodeStatus(nodeName, running, running, null));
            }
        }
        return out;
    }

    private NodeResult installLogOne(
            KubernetesClient k8s, String nsId, String clusterId, String nodeName, String lokiHost) {
        try {
            runJob(
                    k8s,
                    "cmp-fluentbit-install-",
                    nodeName,
                    logInstallScript(nsId, clusterId, nodeName, lokiHost));
            return new NodeResult(nodeName, true, "installed");
        } catch (Exception e) {
            log.error("k8s log agent install failed node={}", nodeName, e);
            return new NodeResult(nodeName, false, e.getMessage());
        }
    }

    private NodeResult uninstallLogOne(KubernetesClient k8s, String nodeName) {
        try {
            runJob(k8s, "cmp-fluentbit-uninstall-", nodeName, logUninstallScript());
            return new NodeResult(nodeName, true, "uninstalled");
        } catch (Exception e) {
            log.error("k8s log agent uninstall failed node={}", nodeName, e);
            return new NodeResult(nodeName, false, e.getMessage());
        }
    }

    /** Loki host = same host as the (VM-reachable) InfluxDB endpoint. */
    private String lokiHost(String nsId, String clusterId) {
        InfluxDTO influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
        try {
            return URI.create(influx.getUrl()).getHost();
        } catch (Exception e) {
            return influx.getUrl();
        }
    }

    private boolean lokiHasRecent(String lokiHost, String nsId, String clusterId, String nodeName) {
        try {
            long endNs = System.currentTimeMillis() * 1_000_000L;
            long startNs = endNs - 180L * 1_000_000_000L;
            String q =
                    "{NS_ID=\""
                            + nsId
                            + "\",INFRA_ID=\""
                            + clusterId
                            + "\",NODE_ID=\""
                            + nodeName
                            + "\"}";
            String url =
                    "http://"
                            + lokiHost
                            + ":"
                            + LOKI_PORT
                            + "/loki/api/v1/query_range?limit=1&start="
                            + startNs
                            + "&end="
                            + endNs
                            + "&query="
                            + enc(q);
            HttpResponse<String> resp =
                    http.send(
                            HttpRequest.newBuilder(URI.create(url))
                                    .timeout(Duration.ofSeconds(10))
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp.body());
            return root.path("data").path("result").isArray()
                    && root.path("data").path("result").size() > 0;
        } catch (Exception e) {
            log.warn("loki recent check failed node={}", nodeName, e);
            return false;
        }
    }

    private String logInstallScript(
            String nsId, String clusterId, String nodeName, String lokiHost) {
        return """
                set -e
                export DEBIAN_FRONTEND=noninteractive
                if ! command -v podman >/dev/null 2>&1; then apt-get update -qq && apt-get install -y -qq podman; fi
                PODMAN=$(command -v podman)
                "$PODMAN" pull %s
                mkdir -p /etc/cmp-fluent-bit
                cat > /etc/cmp-fluent-bit/fluent-bit.conf <<'CONF'
                [SERVICE]
                    flush 5
                    log_level info
                [INPUT]
                    name tail
                    tag k8slog
                    path /var/log/syslog,/var/log/containers/*.log
                    Read_from_Head false
                [OUTPUT]
                    name loki
                    match *
                    host %s
                    port %d
                    labels NS_ID=%s, INFRA_ID=%s, NODE_ID=%s
                CONF
                "$PODMAN" rm -f cmp-fluent-bit >/dev/null 2>&1 || true
                cat > /etc/systemd/system/cmp-fluent-bit.service <<SVC
                [Unit]
                Description=CMP Fluent Bit (k8s node host log agent)
                After=network-online.target
                Wants=network-online.target
                [Service]
                ExecStartPre=-$PODMAN rm -f cmp-fluent-bit
                ExecStart=$PODMAN run --rm --name cmp-fluent-bit --network=host -v /var/log:/var/log:ro -v /etc/cmp-fluent-bit:/etc/cmp-fluent-bit:ro %s -c /etc/cmp-fluent-bit/fluent-bit.conf
                ExecStop=$PODMAN stop -t 10 cmp-fluent-bit
                Restart=always
                RestartSec=5
                [Install]
                WantedBy=multi-user.target
                SVC
                systemctl daemon-reload
                systemctl enable --now cmp-fluent-bit
                """
                .formatted(
                        FLUENT_BIT_IMAGE,
                        lokiHost,
                        LOKI_PORT,
                        nsId,
                        clusterId,
                        nodeName,
                        FLUENT_BIT_IMAGE);
    }

    private String logUninstallScript() {
        return """
                systemctl disable --now cmp-fluent-bit 2>/dev/null || true
                PODMAN=$(command -v podman)
                [ -n "$PODMAN" ] && "$PODMAN" rm -f cmp-fluent-bit 2>/dev/null || true
                rm -f /etc/systemd/system/cmp-fluent-bit.service
                rm -rf /etc/cmp-fluent-bit
                systemctl daemon-reload 2>/dev/null || true
                echo UNINSTALLED
                """;
    }

    public List<NodeStatus> status(String nsId, String clusterId) {
        InfluxDTO influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
        Map<String, String> lastSeen = queryLastSeen(influx, clusterId);
        List<NodeStatus> out = new ArrayList<>();
        try (KubernetesClient k8s = client(nsId, clusterId)) {
            for (Node node : k8s.nodes().list().getItems()) {
                String nodeName = node.getMetadata().getName();
                String ts = lastSeen.get(nodeName);
                boolean running = ts != null && isFresh(ts);
                out.add(new NodeStatus(nodeName, running, running, ts));
            }
        }
        return out;
    }

    // --- Job execution ---------------------------------------------------

    private void runJob(KubernetesClient k8s, String prefix, String nodeName, String script) {
        String b64 = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
        String jobName = prefix + sanitize(nodeName);
        // Clean any stale job with the same name first.
        k8s.batch().v1().jobs().inNamespace(JOB_NS).withName(jobName).delete();

        Job job =
                new JobBuilder()
                        .withNewMetadata()
                        .withName(jobName)
                        .withNamespace(JOB_NS)
                        .endMetadata()
                        .withNewSpec()
                        .withBackoffLimit(0)
                        .withTtlSecondsAfterFinished(300)
                        .withNewTemplate()
                        .withNewSpec()
                        .withNodeName(nodeName)
                        .withHostPID(true)
                        .withHostNetwork(true)
                        .withRestartPolicy("Never")
                        .addNewToleration()
                        .withOperator("Exists")
                        .endToleration()
                        .addNewContainer()
                        .withName("agent")
                        .withImage(NSENTER_IMAGE)
                        .withNewSecurityContext()
                        .withPrivileged(true)
                        .endSecurityContext()
                        .withCommand(
                                "nsenter",
                                "-t",
                                "1",
                                "-m",
                                "-u",
                                "-i",
                                "-n",
                                "-p",
                                "--",
                                "bash",
                                "-c",
                                "echo " + b64 + " | base64 -d | bash")
                        .endContainer()
                        .endSpec()
                        .endTemplate()
                        .endSpec()
                        .build();

        k8s.batch().v1().jobs().inNamespace(JOB_NS).resource(job).create();
        // Best-effort wait for completion.
        try {
            k8s.batch()
                    .v1()
                    .jobs()
                    .inNamespace(JOB_NS)
                    .withName(jobName)
                    .waitUntilCondition(
                            j ->
                                    j != null
                                            && j.getStatus() != null
                                            && (numOrZero(j.getStatus().getSucceeded()) > 0
                                                    || numOrZero(j.getStatus().getFailed()) > 0),
                            180,
                            java.util.concurrent.TimeUnit.SECONDS);
            Job done = k8s.batch().v1().jobs().inNamespace(JOB_NS).withName(jobName).get();
            if (done != null
                    && done.getStatus() != null
                    && numOrZero(done.getStatus().getSucceeded()) == 0) {
                throw new IllegalStateException("job did not succeed: " + jobName);
            }
        } finally {
            k8s.batch()
                    .v1()
                    .jobs()
                    .inNamespace(JOB_NS)
                    .withName(jobName)
                    .withPropagationPolicy(
                            io.fabric8.kubernetes.api.model.DeletionPropagation.BACKGROUND)
                    .delete();
        }
    }

    private static int numOrZero(Integer i) {
        return i == null ? 0 : i;
    }

    private static String sanitize(String s) {
        String v = s.toLowerCase().replaceAll("[^a-z0-9-]", "-");
        return v.length() > 50 ? v.substring(0, 50) : v;
    }

    // --- Scripts ---------------------------------------------------------

    private String installScript(
            String nsId,
            String clusterId,
            String nodeName,
            InfluxDTO influx,
            List<String> metrics) {
        return """
                set -e
                cd /tmp
                curl -sL -o cmp-tg.tar.gz https://dl.influxdata.com/telegraf/releases/telegraf-%s_linux_amd64.tar.gz
                tar xzf cmp-tg.tar.gz
                install -m0755 telegraf-%s/usr/bin/telegraf /usr/local/bin/cmp-telegraf
                mkdir -p /etc/cmp-telegraf
                cat > /etc/cmp-telegraf/telegraf.conf <<'CONF'
                [global_tags]
                  ns_id = "%s"
                  infra_id = "%s"
                  node_id = "%s"
                [agent]
                  interval = "30s"
                  flush_interval = "30s"
                [[outputs.influxdb]]
                  urls = ["%s"]
                  database = "%s"
                  username = "%s"
                  password = "%s"
                %s
                CONF
                cat > /etc/systemd/system/cmp-telegraf.service <<'SVC'
                [Unit]
                Description=CMP Telegraf (k8s node host agent)
                After=network.target
                [Service]
                ExecStart=/usr/local/bin/cmp-telegraf --config /etc/cmp-telegraf/telegraf.conf
                Restart=always
                [Install]
                WantedBy=multi-user.target
                SVC
                systemctl daemon-reload
                systemctl enable --now cmp-telegraf
                """
                .formatted(
                        TELEGRAF_VERSION,
                        TELEGRAF_VERSION,
                        nsId,
                        clusterId,
                        nodeName,
                        influx.getUrl(),
                        influx.getDatabase(),
                        influx.getUsername() == null ? "" : influx.getUsername(),
                        influx.getPassword() == null ? "" : influx.getPassword(),
                        metricsToToml(metrics));
    }

    private String metricsToToml(List<String> metrics) {
        List<String> use = (metrics == null || metrics.isEmpty()) ? DEFAULT_METRICS : metrics;
        StringBuilder sb = new StringBuilder();
        for (String m : use) {
            String toml = INPUTS.get(m);
            if (toml != null) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(toml);
            }
        }
        return sb.toString();
    }

    private String uninstallScript() {
        return """
                systemctl disable --now cmp-telegraf 2>/dev/null || true
                rm -f /etc/systemd/system/cmp-telegraf.service /usr/local/bin/cmp-telegraf
                rm -rf /etc/cmp-telegraf
                systemctl daemon-reload 2>/dev/null || true
                echo UNINSTALLED
                """;
    }

    // --- InfluxDB status query ------------------------------------------

    /** Returns node_id -> last cpu point ISO time for the cluster. */
    private Map<String, String> queryLastSeen(InfluxDTO influx, String clusterId) {
        Map<String, String> map = new java.util.HashMap<>();
        try {
            String q =
                    "SELECT last(\"usage_idle\") FROM cpu WHERE infra_id='"
                            + clusterId
                            + "' GROUP BY node_id";
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(influxGet(influx, q));
            for (var stmt : root.path("results")) {
                for (var series : stmt.path("series")) {
                    String node = series.path("tags").path("node_id").asText(null);
                    var values = series.path("values");
                    if (node != null && values.isArray() && values.size() > 0) {
                        map.put(node, values.get(values.size() - 1).get(0).asText(null));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("k8s agent status influx query failed for cluster={}", clusterId, e);
        }
        return map;
    }

    private String influxGet(InfluxDTO influx, String q) throws Exception {
        String url = influx.getUrl() + "/query?db=" + enc(influx.getDatabase()) + "&q=" + enc(q);
        HttpRequest.Builder rb =
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET();
        if (influx.getUsername() != null && !influx.getUsername().isBlank()) {
            String basic =
                    Base64.getEncoder()
                            .encodeToString(
                                    (influx.getUsername() + ":" + influx.getPassword())
                                            .getBytes(StandardCharsets.UTF_8));
            rb.header("Authorization", "Basic " + basic);
        }
        return http.send(rb.build(), HttpResponse.BodyHandlers.ofString()).body();
    }

    private boolean influxHasSeries(InfluxDTO influx, String q) {
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper()
                            .readTree(influxGet(influx, q));
            for (var stmt : root.path("results")) {
                for (var series : stmt.path("series")) {
                    var values = series.path("values");
                    if (values.isArray() && values.size() > 0) {
                        var first = values.get(0);
                        for (int i = 1; i < first.size(); i++) {
                            if (!first.get(i).isNull()) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("influx hasSeries failed q={}", q, e);
        }
        return false;
    }

    private boolean isFresh(String iso) {
        try {
            java.time.Instant t = java.time.Instant.parse(iso);
            return java.time.Instant.now().minusSeconds(FRESH_SECONDS).isBefore(t);
        } catch (Exception e) {
            return false;
        }
    }

    private static String enc(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
