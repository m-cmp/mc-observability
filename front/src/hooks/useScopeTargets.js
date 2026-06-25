import { useState, useEffect } from 'react';
import { getInfraList, getInfra } from '../api/tumblebug';
import { getK8sClusters, getK8sAgentStatus } from '../api/k8sAgent';

/**
 * Combined scope targets for pickers that select an (infra/cluster, node) pair.
 *
 * VM infras and K8s clusters share the same InfluxDB tag schema
 * (infra_id = infraId|clusterId, node_id = nodeName), so both plug into the same
 * downstream (infra, node) monitoring/insight/alert APIs. This hook returns both
 * lists plus a loading flag so callers can render a grouped picker.
 */
export default function useScopeTargets(nsId) {
  const [infras, setInfras] = useState([]); // [{id,name}]
  const [clusters, setClusters] = useState([]); // [{id,name}]
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!nsId) { setInfras([]); setClusters([]); return; }
    let alive = true;
    setLoading(true);
    Promise.allSettled([getInfraList(nsId), getK8sClusters(nsId)])
      .then(([a, b]) => {
        if (!alive) return;
        setInfras(a.status === 'fulfilled' && Array.isArray(a.value) ? a.value : []);
        setClusters(b.status === 'fulfilled' && Array.isArray(b.value) ? b.value : []);
      })
      .finally(() => { if (alive) setLoading(false); });
    return () => { alive = false; };
  }, [nsId]);

  return { infras, clusters, loading };
}

/**
 * Load nodes for a selected target. K8s clusters resolve to their real nodes (running + powered-off
 * ones recovered from metric history) so insight scope can target a node even while the cluster is
 * stopped — only synthesized placeholder rows (no real name / no data) are dropped. Powered-off
 * nodes are labelled so the picker shows their state.
 */
export async function loadScopeNodes(nsId, targetId, isK8s) {
  if (!nsId || !targetId) return [];
  if (isK8s) {
    const st = await getK8sAgentStatus(nsId, targetId);
    return (Array.isArray(st) ? st : [])
      .filter((n) => !n.placeholder)
      .map((n) => ({ id: n.node, name: n.running ? n.node : `${n.node} (stopped)` }));
  }
  const d = await getInfra(nsId, targetId);
  return d.node || [];
}
