package com.mcmp.o11ymanager.manager.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mcmp.o11ymanager.manager.dto.SpiderClusterInfo;
import com.mcmp.o11ymanager.manager.dto.influx.InfluxDTO;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugK8sCluster;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugK8sToken;
import com.mcmp.o11ymanager.manager.facade.InfluxDbFacadeService;
import com.mcmp.o11ymanager.manager.infrastructure.spider.SpiderClient;
import com.mcmp.o11ymanager.manager.infrastructure.tumblebug.TumblebugClient;
import io.fabric8.kubernetes.api.model.Node;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SpiderClient spiderClient;

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

    /** Log agent: Fluent Bit installed as a host binary (official OS package), shipping to Loki. */
    private static final int LOKI_PORT = 3100;

    private final HttpClient http =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    // Per-node status is expensive: a live cb-spider getCluster (CSP round-trip) plus InfluxDB
    // cardinality/last-seen queries, run once per cluster. The UI polls every cluster's agent and
    // log-agent status on a short interval, so without caching those slow calls pile up in the
    // Tomcat queue and the whole UI stalls behind them. A short TTL collapses a burst of polls into
    // a single backend pass while keeping status fresh enough to be useful.
    private final Cache<String, List<NodeStatus>> statusCache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(15)).maximumSize(500).build();
    private final Cache<String, List<NodeStatus>> logStatusCache =
            Caffeine.newBuilder().expireAfterWrite(Duration.ofSeconds(15)).maximumSize(500).build();

    @Getter
    @lombok.AllArgsConstructor
    public static class NodeResult {
        private String node;
        private boolean ok;
        private String message;
    }

    /** Whether a node host is powered on. Serialized as "RUNNING"/"STOPPED". */
    public enum PowerState {
        RUNNING,
        STOPPED;

        static PowerState of(boolean up) {
            return up ? RUNNING : STOPPED;
        }
    }

    @Getter
    @lombok.AllArgsConstructor
    public static class NodeStatus {
        private String node;
        private boolean installed; // agent has reported at some point (telegraf present)
        private boolean running; // reported to InfluxDB recently
        private String lastSeen; // ISO timestamp or null
        private PowerState powerState; // node host power state
        private boolean placeholder; // synthesized stopped node (real name not retrievable)
    }

    /** One node discovered for a cluster, with whether its host is currently up. */
    private static class NodeRef {
        final String name;
        final boolean running; // host powered on (present in cb-spider node list or reporting)
        final boolean placeholder;

        NodeRef(String name, boolean running, boolean placeholder) {
            this.name = name;
            this.running = running;
            this.placeholder = placeholder;
        }
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
        String kubeconfig = cluster.getAccessInfo().getKubeconfig();

        // cb-spider hands out exec-plugin kubeconfigs for AWS EKS / GCP GKE / NCP NKS that shell
        // out to a cloud CLI (or cb-spider's local credential via 0.0.0.0:1024) to mint a token.
        // Neither the CLI nor that credential exists in this container, and fabric8 cannot even
        // parse the exec stanza (YAML "mapping values are not allowed here"). Mirror
        // cm-grasshopper:
        // pull a short-lived bearer token from cb-tumblebug's /token endpoint and build the client
        // from the API server + CA + token, dropping the exec stanza entirely. Self-contained
        // kubeconfigs (e.g. Azure AKS) carry their own credentials and are used as-is.
        if (kubeconfig.contains("exec:")) {
            String server = extractKubeconfigField(kubeconfig, "server");
            if (server == null) {
                throw new IllegalStateException(
                        "could not extract API server from kubeconfig for "
                                + nsId
                                + "/"
                                + clusterId);
            }
            String caData = extractKubeconfigField(kubeconfig, "certificate-authority-data");
            String token = resolveClusterToken(nsId, clusterId);
            ConfigBuilder b = new ConfigBuilder().withMasterUrl(server).withOauthToken(token);
            if (caData != null && !caData.isBlank()) {
                b.withCaCertData(caData);
            } else {
                b.withTrustCerts(true);
            }
            return new KubernetesClientBuilder().withConfig(b.build()).build();
        }

        Config cfg = Config.fromKubeconfig(kubeconfig);
        return new KubernetesClientBuilder().withConfig(cfg).build();
    }

    /** Resolves a short-lived bearer token for an exec-plugin cluster via cb-tumblebug. */
    private String resolveClusterToken(String nsId, String clusterId) {
        TumblebugK8sToken t = tumblebugClient.getK8sClusterToken(nsId, clusterId);
        String token = t == null ? null : t.getToken();
        if (token == null || token.isBlank()) {
            throw new IllegalStateException(
                    "cb-tumblebug returned no token for cluster " + nsId + "/" + clusterId);
        }
        return token;
    }

    /**
     * Pulls a single scalar field (e.g. {@code server}, {@code certificate-authority-data}) out of
     * a kubeconfig by line, without YAML-parsing the exec-plugin user section that fabric8 rejects.
     */
    private static String extractKubeconfigField(String kubeconfig, String key) {
        java.util.regex.Matcher m =
                java.util.regex.Pattern.compile(
                                "(?m)^\\s*"
                                        + java.util.regex.Pattern.quote(key)
                                        + ":\\s*(\\S+)\\s*$")
                        .matcher(kubeconfig);
        return m.find() ? m.group(1) : null;
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
        InfluxDTO influx;
        try {
            influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
        } catch (Exception e) {
            return new ArrayList<>(); // no influx target yet → nothing active
        }
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
        return logStatusCache.get(nsId + "/" + clusterId, k -> computeLogStatus(nsId, clusterId));
    }

    private List<NodeStatus> computeLogStatus(String nsId, String clusterId) {
        String lokiHost = null;
        Map<String, String> historical = Map.of();
        try {
            lokiHost = lokiHost(nsId, clusterId);
            InfluxDTO influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
            historical = queryLastSeen(influx, clusterId);
        } catch (Exception e) {
            log.warn(
                    "k8s log agent status: loki/influxdb unavailable for {}/{} — listing nodes"
                            + " without log history",
                    nsId,
                    clusterId);
        }
        List<NodeStatus> out = new ArrayList<>();
        for (NodeRef ref : discoverNodes(nsId, clusterId, historical)) {
            boolean logging =
                    lokiHost != null
                            && !ref.placeholder
                            && lokiHasRecent(lokiHost, nsId, clusterId, ref.name);
            PowerState power = PowerState.of(ref.running || logging);
            out.add(new NodeStatus(ref.name, logging, logging, null, power, ref.placeholder));
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
        // Install Fluent Bit as a host binary via its official OS package (the install.sh detects
        // apt/dnf/yum and adds the right repo). This works across Ubuntu and Amazon Linux 2023 —
        // unlike the previous podman approach, since EKS AL2023 nodes have no podman in their
        // repos.
        return """
                set -e
                if [ ! -x /opt/fluent-bit/bin/fluent-bit ] && ! command -v fluent-bit >/dev/null 2>&1; then
                  curl -fsSL https://raw.githubusercontent.com/fluent/fluent-bit/master/install.sh -o /tmp/cmp-fbi.sh
                  sh /tmp/cmp-fbi.sh
                fi
                FB=$(command -v fluent-bit || echo /opt/fluent-bit/bin/fluent-bit)
                mkdir -p /etc/cmp-fluent-bit
                cat > /etc/cmp-fluent-bit/fluent-bit.conf <<'CONF'
                [SERVICE]
                    flush 5
                    log_level info
                [INPUT]
                    name tail
                    tag k8slog
                    path /var/log/messages,/var/log/syslog,/var/log/containers/*.log
                    Read_from_Head false
                [OUTPUT]
                    name loki
                    match *
                    host %s
                    port %d
                    labels NS_ID=%s, INFRA_ID=%s, NODE_ID=%s
                CONF
                cat > /etc/systemd/system/cmp-fluent-bit.service <<SVC
                [Unit]
                Description=CMP Fluent Bit (k8s node host log agent)
                After=network-online.target
                Wants=network-online.target
                [Service]
                ExecStart=$FB -c /etc/cmp-fluent-bit/fluent-bit.conf
                Restart=always
                RestartSec=5
                [Install]
                WantedBy=multi-user.target
                SVC
                systemctl daemon-reload
                systemctl enable --now cmp-fluent-bit
                """
                .formatted(lokiHost, LOKI_PORT, nsId, clusterId, nodeName);
    }

    private String logUninstallScript() {
        return """
                systemctl disable --now cmp-fluent-bit 2>/dev/null || true
                rm -f /etc/systemd/system/cmp-fluent-bit.service
                rm -rf /etc/cmp-fluent-bit
                systemctl daemon-reload 2>/dev/null || true
                echo UNINSTALLED
                """;
    }

    public List<NodeStatus> status(String nsId, String clusterId) {
        return statusCache.get(nsId + "/" + clusterId, k -> computeStatus(nsId, clusterId));
    }

    private List<NodeStatus> computeStatus(String nsId, String clusterId) {
        // InfluxDB may be unresolvable (e.g. a cluster that never had an agent / no monitoring
        // target). That just means no agent history — still list the nodes from cb-spider.
        Map<String, String> lastSeen = Map.of();
        try {
            InfluxDTO influx = influxDbFacadeService.resolveForVM(nsId, clusterId);
            lastSeen = queryLastSeen(influx, clusterId);
        } catch (Exception e) {
            log.warn(
                    "k8s agent status: influxdb unavailable for {}/{} — listing nodes without"
                            + " agent history",
                    nsId,
                    clusterId);
        }
        List<NodeStatus> out = new ArrayList<>();
        for (NodeRef ref : discoverNodes(nsId, clusterId, lastSeen)) {
            String ts = ref.placeholder ? null : lastSeen.get(ref.name);
            boolean reporting = ts != null && isFresh(ts);
            // host is up if cb-spider lists it OR it is actively reporting metrics
            PowerState power = PowerState.of(ref.running || reporting);
            // "installed" = the agent has reported at least once (so it was installed on this node)
            boolean installed = ts != null;
            out.add(new NodeStatus(ref.name, installed, reporting, ts, power, ref.placeholder));
        }
        return out;
    }

    /**
     * Enumerates a cluster's nodes from cb-spider node groups so the node list survives the cluster
     * being powered off (the kubeconfig/k8s API is unreachable then, and cb-spider returns node
     * groups with {@code Nodes == null}).
     *
     * <p>When the cluster is up, cb-spider's live node list is authoritative. When it is down,
     * nodes are recovered by their real names from InfluxDB history ({@code historyLastSeen}: node
     * -> last-seen ISO time), keeping only the {@code DesiredNodeSize} most-recently-seen ones (so
     * old names left over from a previous cluster generation don't pile up). Any shortfall against
     * {@code DesiredNodeSize} is filled with powered-off placeholder rows.
     */
    private List<NodeRef> discoverNodes(
            String nsId, String clusterId, Map<String, String> historyLastSeen) {
        Set<String> running = new LinkedHashSet<>();
        List<String> ngNames = new ArrayList<>();
        int desired = 0;
        try {
            TumblebugK8sCluster cluster = tumblebugClient.getK8sCluster(nsId, clusterId);
            String conn = cluster == null ? null : cluster.getConnectionName();
            String cspName = cluster == null ? null : cluster.getCspResourceName();
            if (conn != null && cspName != null) {
                SpiderClusterInfo info = spiderClient.getCluster(cspName, conn);
                if (info != null && info.getNodeGroupList() != null) {
                    for (SpiderClusterInfo.NodeGroup ng : info.getNodeGroupList()) {
                        if (ng.getIId() != null && ng.getIId().getNameId() != null) {
                            ngNames.add(ng.getIId().getNameId());
                        }
                        if (ng.getNodes() != null) {
                            for (SpiderClusterInfo.IId n : ng.getNodes()) {
                                if (n.getNameId() != null) {
                                    running.add(n.getNameId());
                                }
                            }
                        }
                        if (ng.getDesiredNodeSize() != null) {
                            desired += ng.getDesiredNodeSize();
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("k8s node discovery via cb-spider failed cluster={}/{}", nsId, clusterId, e);
        }

        LinkedHashMap<String, NodeRef> ordered = new LinkedHashMap<>();
        if (!running.isEmpty()) {
            // Cluster is up: cb-spider's node list is authoritative (ignore stale history names).
            for (String n : running) {
                ordered.put(n, new NodeRef(n, true, false));
            }
        } else {
            // Cluster is down: recover names from history, most-recently-seen first, capped to the
            // desired node count so dead nodes from an earlier generation don't accumulate.
            List<String> recent = new ArrayList<>(historyLastSeen.keySet());
            recent.sort(
                    (a, b) ->
                            nullSafe(historyLastSeen.get(b))
                                    .compareTo(nullSafe(historyLastSeen.get(a))));
            int limit = desired > 0 ? Math.min(desired, recent.size()) : recent.size();
            for (int i = 0; i < limit; i++) {
                ordered.put(recent.get(i), new NodeRef(recent.get(i), false, false));
            }
        }
        // Fill the remaining (desired - known) nodes as powered-off placeholders.
        String prefix = ngNames.size() == 1 ? ngNames.get(0) : "node";
        int idx = 1;
        while (ordered.size() < desired) {
            String ph = prefix + " #" + idx++;
            if (!ordered.containsKey(ph)) {
                ordered.put(ph, new NodeRef(ph, false, true));
            }
        }
        return new ArrayList<>(ordered.values());
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    // --- Job execution ---------------------------------------------------

    /**
     * Maps a cb-spider node identifier to the actual Kubernetes node name so a Job can be scheduled
     * onto it. On AWS EKS, cb-spider reports the EC2 instance id (e.g. {@code i-0abc...}) while the
     * k8s node name is the private DNS name; the node's {@code spec.providerID} ({@code
     * aws:///<az>/<instance-id>}) links the two. Returns {@code nodeName} unchanged when it already
     * matches a k8s node (e.g. Azure AKS) or no mapping is found.
     */
    private String resolveK8sNodeName(KubernetesClient k8s, String nodeName) {
        try {
            List<Node> nodes = k8s.nodes().list().getItems();
            for (Node n : nodes) {
                if (nodeName.equals(n.getMetadata().getName())) {
                    return nodeName;
                }
            }
            for (Node n : nodes) {
                String pid = n.getSpec() == null ? null : n.getSpec().getProviderID();
                if (pid != null && (pid.endsWith("/" + nodeName) || pid.endsWith(":" + nodeName))) {
                    return n.getMetadata().getName();
                }
            }
            // Azure AKS: cb-spider names the node "<vmss>_<index>" (with an underscore, which is
            // not
            // even a valid k8s node name) while the real node name is the VMSS instance DNS
            // ("<vmss>00000<index>"). Link them through the providerID
            // (.../virtualMachineScaleSets/<vmss>/virtualMachines/<index>).
            java.util.regex.Pattern vmss =
                    java.util.regex.Pattern.compile(
                            "virtualMachineScaleSets/([^/]+)/virtualMachines/([^/]+)");
            for (Node n : nodes) {
                String pid = n.getSpec() == null ? null : n.getSpec().getProviderID();
                if (pid == null) {
                    continue;
                }
                java.util.regex.Matcher m = vmss.matcher(pid);
                if (m.find() && (m.group(1) + "_" + m.group(2)).equals(nodeName)) {
                    return n.getMetadata().getName();
                }
            }
        } catch (Exception e) {
            log.warn("resolveK8sNodeName failed for node={}: {}", nodeName, e.toString());
        }
        return nodeName;
    }

    /**
     * Builds a Job name that stays within Kubernetes' 63-character label limit. The Job controller
     * copies the Job name into the pod template's {@code job-name} label, so an over-long name
     * (e.g. EKS {@code ip-…-ap-northeast-2.compute.internal} or NHN {@code
     * …-default-worker-node-0}) makes the API server reject the Job with "spec.template.labels …
     * must be no more than 63 characters". Keep it unique by appending a short hash of the full
     * node name when truncation is needed.
     */
    private static String boundedJobName(String prefix, String nodeName) {
        String full = prefix + sanitize(nodeName);
        if (full.length() <= 63) {
            return full;
        }
        String hash = Integer.toHexString(nodeName.hashCode());
        int keep = 63 - prefix.length() - 1 - hash.length();
        String head = sanitize(nodeName);
        if (keep < 1) {
            head = "";
        } else if (head.length() > keep) {
            head = head.substring(0, keep);
        }
        return (prefix + head + "-" + hash).replaceAll("-+", "-").replaceAll("-+$", "");
    }

    private void runJob(KubernetesClient k8s, String prefix, String nodeName, String script) {
        String b64 = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_8));
        String jobName = boundedJobName(prefix, nodeName);
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
                        .withNodeName(resolveK8sNodeName(k8s, nodeName))
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
