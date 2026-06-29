import { useState, useEffect, useCallback, useRef } from 'react';
import {
  getK8sClusters, getK8sAgentStatus, getK8sLogStatus,
  installK8sNode, uninstallK8sNode, getK8sNodeMetrics,
  installK8sLogNode, uninstallK8sLogNode,
} from '../api/k8sAgent';
import ProviderBadge from './ProviderBadge';

/**
 * Config-menu panel for Kubernetes clusters. Each cluster is a group (like a VM Infra); its nodes
 * are rows with Monitoring Agent (telegraf) and Log Agent (fluent-bit) install state + per-node
 * Install/Uninstall. Clicking a node opens the monitoring metric-selection panel under the group.
 */
export default function K8sAgentPanel({ nsId }) {
  const [clusters, setClusters] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusMap, setStatusMap] = useState({}); // clusterId -> [{node,running}]
  const [logMap, setLogMap] = useState({}); // clusterId -> [{node,running}]
  const [statusLoaded, setStatusLoaded] = useState({}); // clusterId -> bool (agent status checked)
  const [sel, setSel] = useState(null); // { clusterId, node }
  const [metrics, setMetrics] = useState({ available: [], active: [] });
  const [picked, setPicked] = useState(new Set());
  const [metricLoading, setMetricLoading] = useState(false);
  const [busy, setBusy] = useState('');
  const [busyMsg, setBusyMsg] = useState('');
  const [applied, setApplied] = useState(''); // `${clusterId}/${node}` after a successful Apply

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
    let ok = false;
    try {
      const s = await getK8sAgentStatus(nsId, clusterId);
      setStatusMap((m) => ({ ...m, [clusterId]: Array.isArray(s) ? s : [] }));
      ok = true;
    } catch { /* keep previous data + "Checking…" state; the periodic refresh retries */ }
    // Mark "loaded" as soon as the agent status is in — don't let a slow/hanging log-status
    // request (best-effort) keep the whole panel stuck on "Checking agent status…".
    if (ok) setStatusLoaded((m) => ({ ...m, [clusterId]: true }));
    try {
      const l = await getK8sLogStatus(nsId, clusterId);
      setLogMap((m) => ({ ...m, [clusterId]: Array.isArray(l) ? l : [] }));
    } catch { /* log status is best-effort */ }
  }, [nsId]);

  useEffect(() => { clusters.forEach((c) => loadStatus(c.id)); }, [clusters, loadStatus]);

  // Periodically re-check agent status so transient states settle on their own (e.g. after a
  // node restart). Skipped while an install/uninstall is running on a cluster.
  const busyRef = useRef('');
  useEffect(() => { busyRef.current = busy; }, [busy]);
  useEffect(() => {
    if (clusters.length === 0) return;
    const id = setInterval(() => { if (!busyRef.current) clusters.forEach((c) => loadStatus(c.id)); }, 10000);
    return () => clearInterval(id);
  }, [clusters, loadStatus]);

  async function selectNode(clusterId, node) {
    setSel({ clusterId, node });
    setApplied('');
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

  async function run(key, msg, fn, clusterId) {
    setBusy(key); setBusyMsg(msg);
    try { await fn(); await loadStatus(clusterId); }
    catch (e) { alert((e.response?.data?.error_message || e.response?.data?.message || e.message)); }
    setBusy(''); setBusyMsg('');
  }

  const logRunning = (cid, node) => (logMap[cid] || []).find((n) => n.node === node)?.running;

  if (loading) return <div className="bg-white rounded-lg shadow p-4 text-sm text-gray-400 animate-pulse">Loading K8s clusters…</div>;
  if (clusters.length === 0) return null;

  return (
    <>
      {clusters.map((c) => {
        const nodes = statusMap[c.id] || [];
        return (
          <div key={c.id} className="bg-white rounded-lg shadow relative">
            {busy && busy.startsWith(c.id) && (
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
              <span className="text-xs text-gray-400">{statusLoaded[c.id] ? `${nodes.length} Nodes` : 'checking agents…'}</span>
            </div>
            <div className="overflow-auto">
              <table className="w-full text-sm">
                <thead><tr className="bg-gray-50 text-left">
                  <th className="px-4 py-2.5 border-b text-gray-500">Name</th>
                  <th className="px-4 py-2.5 border-b text-gray-500">Monitoring Agent</th>
                  <th className="px-4 py-2.5 border-b text-gray-500">Log Agent</th>
                </tr></thead>
                <tbody>
                  {!statusLoaded[c.id] ? <tr><td colSpan={3} className="px-4 py-6 text-center text-gray-400 animate-pulse">Checking agent status…</td></tr>
                  : nodes.length === 0 ? <tr><td colSpan={3} className="px-4 py-6 text-center text-gray-400">No nodes / status unavailable</td></tr>
                  : nodes.map((n) => {
                    const monOn = n.running;
                    const logOn = logRunning(c.id, n.node);
                    const powered = String(n.powerState || '').toUpperCase() === 'RUNNING';
                    // Agents can only be installed/uninstalled on a powered-on node with a real name.
                    const actionable = powered && !n.placeholder;
                    const selectable = !n.placeholder;
                    return (
                      <tr key={n.node} onClick={() => selectable && selectNode(c.id, n.node)}
                        className={`${selectable ? 'cursor-pointer hover:bg-blue-50' : 'cursor-default'} ${sel?.clusterId === c.id && sel?.node === n.node ? 'bg-blue-100' : ''}`}>
                        <td className="px-4 py-2.5 border-b font-medium">
                          <span className="inline-flex items-center gap-1.5">
                            <ProviderBadge connectionName={c.connectionName} showLabel={false} />
                            <span className={n.placeholder ? 'text-gray-400 italic' : ''}>{n.node}</span>
                            <span className={`text-[11px] px-2 py-0.5 rounded-full ${powered ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{powered ? 'Running' : 'Stopped'}</span>
                          </span>
                        </td>
                        <td className="px-4 py-2.5 border-b" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center gap-2">
                            <AgentBadge running={monOn} installed={n.installed} powered={powered} />
                            {!actionable
                              ? <span className="text-xs text-gray-400">{powered ? '' : 'start cluster to manage'}</span>
                              : !monOn
                              ? <button onClick={() => run(`${c.id}/${n.node}/mon`, `Installing agent on ${n.node}…`, () => installK8sNode(nsId, c.id, n.node, null), c.id)} disabled={!!busy} className="text-xs bg-blue-600 text-white px-2 py-1 rounded hover:bg-blue-700 disabled:opacity-50">Install</button>
                              : <button onClick={() => run(`${c.id}/${n.node}/mon`, `Uninstalling agent from ${n.node}…`, () => uninstallK8sNode(nsId, c.id, n.node), c.id)} disabled={!!busy} className="text-xs text-red-500 hover:text-red-700 disabled:opacity-50">Uninstall</button>}
                          </div>
                        </td>
                        <td className="px-4 py-2.5 border-b" onClick={(e) => e.stopPropagation()}>
                          <div className="flex items-center gap-2">
                            <AgentBadge running={logOn} powered={powered} />
                            {!actionable
                              ? <span className="text-xs text-gray-400">{powered ? '' : 'start cluster to manage'}</span>
                              : !logOn
                              ? <button onClick={() => run(`${c.id}/${n.node}/log`, `Installing log agent on ${n.node}…`, () => installK8sLogNode(nsId, c.id, n.node), c.id)} disabled={!!busy} className="text-xs bg-emerald-600 text-white px-2 py-1 rounded hover:bg-emerald-700 disabled:opacity-50">Install</button>
                              : <button onClick={() => run(`${c.id}/${n.node}/log`, `Uninstalling log agent from ${n.node}…`, () => uninstallK8sLogNode(nsId, c.id, n.node), c.id)} disabled={!!busy} className="text-xs text-red-500 hover:text-red-700 disabled:opacity-50">Uninstall</button>}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {/* Monitoring metric selection for the selected node */}
            {sel?.clusterId === c.id && (
              <div className="border-t bg-gray-50 p-4">
                <div className="font-semibold text-sm mb-3">Monitoring Metrics — {sel.node}</div>
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
                    <button onClick={() => run(`${c.id}/${sel.node}/mon`, `Applying metrics on ${sel.node}…`, async () => {
                        await installK8sNode(nsId, c.id, sel.node, [...picked]);
                        // Keep the user's selection visible. Refresh the available list but do NOT
                        // reset `picked` from InfluxDB-derived "active" — metrics take ~1 min to
                        // start flowing after install, so active would be empty and wrongly clear
                        // every checkbox.
                        try { const m = await getK8sNodeMetrics(nsId, c.id, sel.node); setMetrics((prev) => ({ ...prev, available: m.available || prev.available, active: m.active || [] })); } catch { /* keep current */ }
                        setApplied(`${c.id}/${sel.node}`);
                      }, c.id)}
                      disabled={!!busy || picked.size === 0}
                      className="text-sm bg-blue-600 text-white px-4 py-1.5 rounded hover:bg-blue-700 disabled:opacity-50">
                      Apply (install selected)
                    </button>
                    {applied === `${c.id}/${sel.node}`
                      ? <span className="ml-3 text-xs text-green-600">Applied — the agent is installing; metrics &amp; Running status appear within ~1 min.</span>
                      : <span className="ml-3 text-xs text-gray-400">Re-installs the monitoring agent with the selected inputs.</span>}
                  </>
                )}
              </div>
            )}
          </div>
        );
      })}
    </>
  );
}

function AgentBadge({ running, installed, powered }) {
  if (running) return <span className="text-xs px-2 py-0.5 rounded-full bg-green-100 text-green-700">Running</span>;
  if (installed && powered === false) return <span className="text-xs px-2 py-0.5 rounded-full bg-amber-100 text-amber-700">Installed · off</span>;
  if (powered === false) return <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-400">—</span>;
  return <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500">Not installed</span>;
}
