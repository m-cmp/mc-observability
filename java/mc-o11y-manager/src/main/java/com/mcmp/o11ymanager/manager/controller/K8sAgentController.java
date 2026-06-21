package com.mcmp.o11ymanager.manager.controller;

import com.mcmp.o11ymanager.manager.global.vm.ResBody;
import com.mcmp.o11ymanager.manager.service.K8sAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Host-level Telegraf agent management for Kubernetes cluster nodes. Installs Telegraf as a systemd
 * service on each node host via the cluster kubeconfig (no SSH / node IP required).
 */
@RestController
@RequestMapping("/api/o11y/monitoring/k8s")
@RequiredArgsConstructor
@Tag(name = "K8s Node Agent", description = "Install/uninstall host Telegraf on K8s cluster nodes")
public class K8sAgentController {

    private final K8sAgentService k8sAgentService;

    @Operation(summary = "Install host Telegraf agent on every node of the cluster")
    @PostMapping("/{nsId}/{clusterId}/agent")
    public ResBody<List<K8sAgentService.NodeResult>> install(
            @PathVariable String nsId, @PathVariable String clusterId) {
        return new ResBody<>(k8sAgentService.install(nsId, clusterId));
    }

    @Operation(summary = "Uninstall host Telegraf agent from every node of the cluster")
    @DeleteMapping("/{nsId}/{clusterId}/agent")
    public ResBody<List<K8sAgentService.NodeResult>> uninstall(
            @PathVariable String nsId, @PathVariable String clusterId) {
        return new ResBody<>(k8sAgentService.uninstall(nsId, clusterId));
    }

    @Operation(summary = "Per-node agent status (running = reported to InfluxDB recently)")
    @GetMapping("/{nsId}/{clusterId}/agent")
    public ResBody<List<K8sAgentService.NodeStatus>> status(
            @PathVariable String nsId, @PathVariable String clusterId) {
        return new ResBody<>(k8sAgentService.status(nsId, clusterId));
    }

    @Operation(summary = "Install host Telegraf agent on a single node (optional metric selection)")
    @PostMapping("/{nsId}/{clusterId}/node/{nodeName}/agent")
    public ResBody<K8sAgentService.NodeResult> installNode(
            @PathVariable String nsId,
            @PathVariable String clusterId,
            @PathVariable String nodeName,
            @RequestBody(required = false) Map<String, Object> body) {
        return new ResBody<>(k8sAgentService.installNode(nsId, clusterId, nodeName, metrics(body)));
    }

    @Operation(summary = "Uninstall host Telegraf agent from a single node")
    @DeleteMapping("/{nsId}/{clusterId}/node/{nodeName}/agent")
    public ResBody<K8sAgentService.NodeResult> uninstallNode(
            @PathVariable String nsId,
            @PathVariable String clusterId,
            @PathVariable String nodeName) {
        return new ResBody<>(k8sAgentService.uninstallNode(nsId, clusterId, nodeName));
    }

    @Operation(summary = "Available metric inputs and the ones currently active on the node")
    @GetMapping("/{nsId}/{clusterId}/node/{nodeName}/agent/metrics")
    public ResBody<Map<String, Object>> nodeMetrics(
            @PathVariable String nsId,
            @PathVariable String clusterId,
            @PathVariable String nodeName) {
        return new ResBody<>(
                Map.of(
                        "available", List.copyOf(K8sAgentService.INPUTS.keySet()),
                        "active", k8sAgentService.nodeActiveMetrics(nsId, clusterId, nodeName)));
    }

    // --- Log agent (fluent-bit via podman) ---

    @Operation(summary = "Per-node log agent status (running = node logs in Loki recently)")
    @GetMapping("/{nsId}/{clusterId}/log-agent")
    public ResBody<List<K8sAgentService.NodeStatus>> logStatus(
            @PathVariable String nsId, @PathVariable String clusterId) {
        return new ResBody<>(k8sAgentService.logStatus(nsId, clusterId));
    }

    @Operation(summary = "Install fluent-bit log agent on every node of the cluster")
    @PostMapping("/{nsId}/{clusterId}/log-agent")
    public ResBody<List<K8sAgentService.NodeResult>> installLog(
            @PathVariable String nsId, @PathVariable String clusterId) {
        return new ResBody<>(k8sAgentService.installLog(nsId, clusterId));
    }

    @Operation(summary = "Uninstall fluent-bit log agent from every node of the cluster")
    @DeleteMapping("/{nsId}/{clusterId}/log-agent")
    public ResBody<List<K8sAgentService.NodeResult>> uninstallLog(
            @PathVariable String nsId, @PathVariable String clusterId) {
        return new ResBody<>(k8sAgentService.uninstallLog(nsId, clusterId));
    }

    @Operation(summary = "Install fluent-bit log agent on a single node")
    @PostMapping("/{nsId}/{clusterId}/node/{nodeName}/log-agent")
    public ResBody<K8sAgentService.NodeResult> installLogNode(
            @PathVariable String nsId,
            @PathVariable String clusterId,
            @PathVariable String nodeName) {
        return new ResBody<>(k8sAgentService.installLogNode(nsId, clusterId, nodeName));
    }

    @Operation(summary = "Uninstall fluent-bit log agent from a single node")
    @DeleteMapping("/{nsId}/{clusterId}/node/{nodeName}/log-agent")
    public ResBody<K8sAgentService.NodeResult> uninstallLogNode(
            @PathVariable String nsId,
            @PathVariable String clusterId,
            @PathVariable String nodeName) {
        return new ResBody<>(k8sAgentService.uninstallLogNode(nsId, clusterId, nodeName));
    }

    @SuppressWarnings("unchecked")
    private static List<String> metrics(Map<String, Object> body) {
        if (body == null) {
            return null;
        }
        Object m = body.get("metrics");
        return (m instanceof List) ? (List<String>) m : null;
    }
}
