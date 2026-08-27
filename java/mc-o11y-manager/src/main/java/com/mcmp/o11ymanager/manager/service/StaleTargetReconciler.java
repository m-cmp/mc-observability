package com.mcmp.o11ymanager.manager.service;

import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfra;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugInfraList;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugK8sCluster;
import com.mcmp.o11ymanager.manager.dto.tumblebug.TumblebugNS;
import com.mcmp.o11ymanager.manager.entity.K8sAgentTaskEntity;
import com.mcmp.o11ymanager.manager.entity.VMEntity;
import com.mcmp.o11ymanager.manager.infrastructure.tumblebug.TumblebugClient;
import com.mcmp.o11ymanager.manager.repository.K8sAgentTaskJpaRepository;
import com.mcmp.o11ymanager.manager.repository.VMJpaRepository;
import feign.FeignException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Removes monitoring targets that have been deleted in cb-tumblebug but still linger in the
 * observability DB. Those stale rows keep showing up in the monitoring node list, the monitoring
 * config page, and the alert-setting target dropdowns because {@code node} / {@code k8s_agent_task}
 * rows are only ever inserted (on VM registration / agent install) and are never pruned when the
 * underlying VM or K8s cluster disappears from Tumblebug.
 *
 * <p>Each pass walks every Tumblebug namespace, builds the set of live targets, and diffs it
 * against the persisted rows. To stay safe against id-mapping mistakes or transient Tumblebug
 * errors, a row is deleted <b>only</b> when Tumblebug explicitly confirms it is gone with a 404 on
 * a direct lookup — any other error (429, 5xx, network) leaves the row untouched.
 */
@Slf4j
@Component
@ConditionalOnProperty(
        prefix = "observability.reconcile.stale-targets",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
public class StaleTargetReconciler {

    /** Tumblebug rate-limits ~3 sequential calls (429); space discovery calls out. */
    private static final long TB_CALL_SPACING_MS = 400L;

    private final TumblebugClient tumblebugClient;
    private final VMJpaRepository vmJpaRepository;
    private final K8sAgentTaskJpaRepository k8sAgentTaskJpaRepository;

    @Scheduled(cron = "${observability.reconcile.stale-targets.cron:0 */10 * * * *}")
    public void reconcile() {
        try {
            reconcileOnce();
        } catch (Exception e) {
            log.warn("[STALE-RECONCILE] pass failed: {}", e.toString());
        }
    }

    /** One reconciliation pass. Package-private so it can be triggered directly in tests/admin. */
    void reconcileOnce() {
        TumblebugNS nsList;
        try {
            nsList = tumblebugClient.getNSList();
        } catch (Exception e) {
            log.warn("[STALE-RECONCILE] getNSList failed, skipping pass: {}", e.toString());
            return;
        }
        if (nsList == null || nsList.getNs() == null || nsList.getNs().isEmpty()) {
            return;
        }

        // Per-NS live target keys, populated only for namespaces we could successfully scan.
        Map<String, Set<String>> liveVmKeysByNs = new HashMap<>();
        Map<String, Set<String>> liveClusterIdsByNs = new HashMap<>();
        Set<String> vmScannedNs = new HashSet<>();
        Set<String> k8sScannedNs = new HashSet<>();

        boolean space = false;
        for (TumblebugNS.NS ns : nsList.getNs()) {
            if (ns == null || ns.getId() == null) {
                continue;
            }
            String nsId = ns.getId();

            if (space) {
                sleepQuietly(TB_CALL_SPACING_MS);
            }
            space = true;
            try {
                TumblebugInfraList infraList = tumblebugClient.getInfraList(nsId);
                Set<String> keys = new HashSet<>();
                if (infraList != null && infraList.getInfra() != null) {
                    for (TumblebugInfra infra : infraList.getInfra()) {
                        if (infra == null || infra.getId() == null || infra.getNode() == null) {
                            continue;
                        }
                        for (TumblebugInfra.Node node : infra.getNode()) {
                            if (node != null && node.getId() != null) {
                                keys.add(vmKey(infra.getId(), node.getId()));
                            }
                        }
                    }
                }
                liveVmKeysByNs.put(nsId, keys);
                vmScannedNs.add(nsId);
            } catch (Exception e) {
                log.warn(
                        "[STALE-RECONCILE] getInfraList failed ns={}, skipping VM reconcile: {}",
                        nsId,
                        e.toString());
            }

            sleepQuietly(TB_CALL_SPACING_MS);
            try {
                TumblebugK8sCluster.ListResponse resp = tumblebugClient.getK8sClusterList(nsId);
                Set<String> clusterIds = new HashSet<>();
                if (resp != null && resp.getK8sClusterInfo() != null) {
                    for (TumblebugK8sCluster c : resp.getK8sClusterInfo()) {
                        if (c != null && c.getId() != null) {
                            clusterIds.add(c.getId());
                        }
                    }
                }
                liveClusterIdsByNs.put(nsId, clusterIds);
                k8sScannedNs.add(nsId);
            } catch (Exception e) {
                log.warn(
                        "[STALE-RECONCILE] getK8sClusterList failed ns={}, skipping K8s reconcile: {}",
                        nsId,
                        e.toString());
            }
        }

        int removedVms = pruneStaleVms(vmScannedNs, liveVmKeysByNs);
        int removedK8s = pruneStaleK8sAgentTasks(k8sScannedNs, liveClusterIdsByNs);
        if (removedVms > 0 || removedK8s > 0) {
            log.info(
                    "[STALE-RECONCILE] pruned {} stale node row(s), {} stale k8s agent-task row(s)",
                    removedVms,
                    removedK8s);
        }
    }

    private int pruneStaleVms(Set<String> scannedNs, Map<String, Set<String>> liveKeysByNs) {
        int removed = 0;
        for (VMEntity vm : vmJpaRepository.findAll()) {
            String nsId = vm.getNsId();
            if (nsId == null || !scannedNs.contains(nsId)) {
                continue; // couldn't verify this NS this pass — leave it alone
            }
            if (liveKeysByNs
                    .getOrDefault(nsId, Set.of())
                    .contains(vmKey(vm.getInfraId(), vm.getNodeId()))) {
                continue; // still present in Tumblebug
            }
            // Candidate looks stale — confirm with a direct lookup so a key-mapping or listing
            // glitch can never wipe a VM that actually still exists.
            if (!isGone(() -> tumblebugClient.getNode(nsId, vm.getInfraId(), vm.getNodeId()))) {
                continue;
            }
            vmJpaRepository.delete(vm);
            removed++;
            log.info(
                    "[STALE-RECONCILE] removed stale node ns={} infra={} node={}",
                    nsId,
                    vm.getInfraId(),
                    vm.getNodeId());
        }
        return removed;
    }

    private int pruneStaleK8sAgentTasks(
            Set<String> scannedNs, Map<String, Set<String>> liveClusterIdsByNs) {
        int removed = 0;
        for (K8sAgentTaskEntity task : k8sAgentTaskJpaRepository.findAll()) {
            String nsId = task.getNsId();
            if (nsId == null || !scannedNs.contains(nsId)) {
                continue;
            }
            if (liveClusterIdsByNs.getOrDefault(nsId, Set.of()).contains(task.getClusterId())) {
                continue;
            }
            if (!isGone(() -> tumblebugClient.getK8sCluster(nsId, task.getClusterId()))) {
                continue;
            }
            k8sAgentTaskJpaRepository.delete(task);
            removed++;
            log.info(
                    "[STALE-RECONCILE] removed stale k8s agent-task ns={} cluster={} node={}",
                    nsId,
                    task.getClusterId(),
                    task.getNodeName());
        }
        return removed;
    }

    /**
     * Returns true only when Tumblebug definitively reports the resource as gone (HTTP 404). Any
     * other outcome (success, 429, 5xx, network error) returns false so we never delete on
     * uncertainty.
     */
    private boolean isGone(Runnable lookup) {
        try {
            lookup.run();
            return false;
        } catch (FeignException.NotFound nf) {
            return true;
        } catch (Exception e) {
            log.debug(
                    "[STALE-RECONCILE] existence check inconclusive, keeping row: {}",
                    e.toString());
            return false;
        }
    }

    private static String vmKey(String infraId, String nodeId) {
        return infraId + " " + nodeId;
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
