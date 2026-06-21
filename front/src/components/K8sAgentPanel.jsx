import { useState, useEffect, useCallback } from 'react';
import {
  getK8sClusters, getK8sAgentStatus,
  installK8sNode, uninstallK8sNode, getK8sNodeMetrics,
} from '../api/k8sAgent';
import ProviderBadge from './ProviderBadge';

/**
 * Config-menu panel for Kubernetes clusters. Each cluster is shown as a group (like a VM Infra)
 * and its nodes as rows with per-node install state and Install/Uninstall actions. Clicking a node
 * opens a metric-selection panel directly under the group (like VM nodes).
 */
export default function K8sAgentPanel({ nsId }) {
  const [clusters, setClusters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusMap, setStatusMap] = useState({}); // clusterId -> [{node,running,lastSeen}]
  const [sel, setSel] = useState(null); // { clusterId, node }
  const [metrics, setMetrics] = useState({ available: [], active: [] });
  const [picked, setPicked] = useState(new Set());
  const [metricLoading, setMetricLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [busyMsg, setBusyMsg] = useState('');

  const load = useCallback(() => {
    if (!nsId) { setClusters([]); setLoading(false); return; }
    setLoading(true);
    getK8sClusters(nsId)
      .then((cs) => setClusters(Array.isArray(cs) ? cs : []))
      .catch(() => setClusters([]))
      .finally(() => setLoading(false));
  }, [nsId]);

  useEffect(() => { load(); }, [load]);

  const loadStatus = useCallback(async (clusterId) => {
    try {
      const s = await getK8sAgentStatus(nsId, clusterId);
      setStatusMap((m) => ({ ...m, [clusterId]: Array.isArray(s) ? s : [] }));
    } catch { setStatusMap((m) => ({ ...m, [clusterId]: [] })); }
  }, [nsId]);

  useEffect(() => { clusters.forEach((c) => loadStatus(c.id)); }, [clusters, loadStatus]);

  async function selectNode(clusterId, node) {
    setSel({ clusterId, node });
    setMetricLoading(true);
    setMetrics({ available: [], active: [] });
    try {
      const m = await getK8sNodeMetrics(nsId, clusterId, node);
      setMetrics(m);
      setPicked(new Set(m.active || []));
    } catch { setMetrics({ available: [], active: [] }); setPicked(new Set()); }
    setMetricLoading(false);
  }

  function togglePick(name) {
    setPicked((p) => { const n = new Set(p); n.has(name) ? n.delete(name) : n.add(name); return n; });
  }

  async function applyInstall(clusterId, node) {
    setBusy(true); setBusyMsg(`Installing agent on ${node}…`);
    try {
      await installK8sNode(nsId, clusterId, node, [...picked]);
      await loadStatus(clusterId);
      setTimeout(() => selectNode(clusterId, node), 1000);
    } catch (e) { alert('Install failed: ' + (e.response?.data?.error_message || e.response?.data?.message || e.message)); }
    setBusy(false); setBusyMsg('');
  }

  async function doUninstall(clusterId, node) {
    if (!confirm(`Uninstall the agent from node "${node}"?`)) return;
    setBusy(true); setBusyMsg(`Uninstalling agent from ${node}…`);
    try {
      await uninstallK8sNode(nsId, clusterId, node);
      await loadStatus(clusterId);
      if (sel?.clusterId === clusterId && sel?.node === node) setSel(null);
    } catch (e) { alert('Uninstall failed: ' + (e.response?.data?.error_message || e.response?.data?.message || e.message)); }
    setBusy(false); setBusyMsg('');
  }

  if (loading) return null;
  if (clusters.length === 0) return null;

  return (
    <>
      {clusters.map((c) => {
        const nodes = statusMap[c.id] || [];
        return (
          <div key={c.id} className="bg-white rounded-lg shadow relative">
            {busy && sel?.clusterId === c.id && (
              <div className="absolute inset-0 bg-white/60 backdrop-blur-sm z-10 flex items-center justify-center rounded-lg">
                <div className="text-center">
                  <div className="w-7 h-7 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
                  <p className="text-sm text-gray-700">{busyMsg}</p>
                </div>
              </div>
            )}
            <div className="px-4 py-3 border-b flex items-center gap-3">
              <span className="text-xs px-1.5 py-0.5 rounded bg-indigo-100 text-indigo-700 font-medium">K8s</span>
              <span className="font-semibold text-sm">{c.name || c.id}</span>
              <span className={`text-xs px-2 py-0.5 rounded-full ${(c.status || '').includes('Active') ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{c.status || '-'}</span>
              <span className="text-xs text-gray-400">{nodes.length} Nodes</span>
            </div>
            <div className="overflow-auto">
              <table className="w-full text-sm">
                <thead><tr className="bg-gray-50 text-left">
                  <th className="px-4 py-2.5 border-b text-gray-500">Name</th>
                  <th className="px-4 py-2.5 border-b text-gray-500">Monitoring Agent</th>
                  <th className="px-4 py-2.5 border-b text-gray-500">Last Seen</th>
                  <th className="px-4 py-2.5 border-b text-gray-500 text-right">Actions</th>
                </tr></thead>
                <tbody>
                  {nodes.length === 0 ? <tr><td colSpan={4} className="px-4 py-6 text-center text-gray-400">No nodes / status unavailable</td></tr>
                  : nodes.map((n) => (
                    <tr key={n.node} onClick={() => selectNode(c.id, n.node)}
                      className={`cursor-pointer hover:bg-blue-50 ${sel?.clusterId === c.id && sel?.node === n.node ? 'bg-blue-100' : ''}`}>
                      <td className="px-4 py-2.5 border-b font-medium"><span className="inline-flex items-center gap-1.5"><ProviderBadge connectionName={c.connectionName} showLabel={false} />{n.node}</span></td>
                      <td className="px-4 py-2.5 border-b">
                        {n.running ? <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700">Running</span>
                          : <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">Not installed</span>}
                      </td>
                      <td className="px-4 py-2.5 border-b text-gray-500 text-xs">{n.lastSeen || '-'}</td>
                      <td className="px-4 py-2.5 border-b text-right whitespace-nowrap">
                        {!n.running
                          ? <button onClick={(e) => { e.stopPropagation(); applyInstallDefault(c.id, n.node); }} disabled={busy} className="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 disabled:opacity-50">Install Agent</button>
                          : <button onClick={(e) => { e.stopPropagation(); doUninstall(c.id, n.node); }} disabled={busy} className="text-sm text-red-500 hover:text-red-700 disabled:opacity-50">Uninstall</button>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Metric selection panel for the selected node of THIS cluster */}
            {sel?.clusterId === c.id && (
              <div className="border-t bg-gray-50 p-4">
                <div className="font-semibold text-sm mb-3">Metrics — {sel.node}</div>
                {metricLoading ? <p className="text-sm text-gray-400 animate-pulse">Loading metrics…</p> : (
                  <>
                    <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 mb-3">
                      {(metrics.available || []).map((m) => {
                        const on = picked.has(m);
                        return (
                          <label key={m} className="flex items-center gap-2 text-sm bg-white border rounded px-3 py-2 cursor-pointer">
                            <input type="checkbox" checked={on} onChange={() => togglePick(m)} />
                            <span>{m}</span>
                            {(metrics.active || []).includes(m) && <span className="ml-auto text-xs text-green-600">active</span>}
                          </label>
                        );
                      })}
                    </div>
                    <button onClick={() => applyInstall(c.id, sel.node)} disabled={busy || picked.size === 0}
                      className="text-sm bg-blue-600 text-white px-4 py-1.5 rounded hover:bg-blue-700 disabled:opacity-50">
                      Apply (install selected)
                    </button>
                    <span className="ml-3 text-xs text-gray-400">Re-installs the node agent with the selected inputs.</span>
                  </>
                )}
              </div>
            )}
          </div>
        );
      })}
    </>
  );

  function applyInstallDefault(clusterId, node) {
    // Install with the default full metric set (then user can refine via the panel).
    setBusy(true); setBusyMsg(`Installing agent on ${node}…`);
    installK8sNode(nsId, clusterId, node, null)
      .then(() => loadStatus(clusterId))
      .then(() => setTimeout(() => selectNode(clusterId, node), 1000))
      .catch((e) => alert('Install failed: ' + (e.response?.data?.error_message || e.response?.data?.message || e.message)))
      .finally(() => { setBusy(false); setBusyMsg(''); });
  }
}
