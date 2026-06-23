import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getNodeList, getNode, getNodeItems, installAgent, uninstallAgent, createNodeItem, deleteNodeItem } from '../api/node';
import { getPlugins } from '../api/monitoring';
import { getInfra, getInfraList } from '../api/tumblebug';
import ProviderBadge from '../components/ProviderBadge';
import K8sAgentPanel from '../components/K8sAgentPanel';
import { nodeRunState } from '../utils/nodeState';

export default function MonitoringConfig() {
  const { nsId, infraId } = useParams();
  const [allInfras, setAllInfras] = useState([]); // NS-level Infra list with merged Node data
  const [nodes, setNodes] = useState([]); // flat Node list for single Infra mode
  const [filter, setFilter] = useState('');
  const [selectedNode, setSelectedNode] = useState(null);
  const [selectedNodeInfraId, setSelectedNodeInfraId] = useState(infraId || '');
  const [items, setItems] = useState([]);
  const [picked, setPicked] = useState(new Set()); // pluginSeq selected in the metric panel
  const [plugins, setPlugins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [itemLoading, setItemLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [busyMsg, setBusyMsg] = useState('');
  const [globalBusy, setGlobalBusy] = useState(false);
  const pollRef = useRef(null);

  const loadNodes = useCallback(async (silent = false) => {
    if (!nsId) return;
    if (!silent) setLoading(true);
    try {
      if (infraId) {
        // Single Infra mode
        const infraData = await getInfra(nsId, infraId);
        const tbNodes = infraData.node || [];
        let o11yNodes = [];
        try { o11yNodes = await getNodeList(nsId, infraId); } catch {}
        const o11yMap = {};
        o11yNodes.forEach((n) => { o11yMap[n.node_id || n.id] = n; });
        const merged = tbNodes.map((node) => {
          const o = o11yMap[node.id] || {};
          return { ...node, infraId, monitoring_agent_status: o.monitoring_agent_status || null, log_agent_status: o.log_agent_status || null, registered: !!o11yMap[node.id] };
        });
        setNodes(merged);
        setAllInfras([{ id: infraId, name: infraId, node: merged, status: infraData.status }]);
      } else {
        // NS level: all Infras
        const infras = await getInfraList(nsId);
        const enriched = await Promise.all(infras.map(async (infra) => {
          let o11yNodes = [];
          try { o11yNodes = await getNodeList(nsId, infra.id); } catch {}
          const o11yMap = {};
          o11yNodes.forEach((n) => { o11yMap[n.node_id || n.id] = n; });
          const merged = (infra.node || []).map((node) => {
            const o = o11yMap[node.id] || {};
            return { ...node, infraId: infra.id, monitoring_agent_status: o.monitoring_agent_status || null, log_agent_status: o.log_agent_status || null, registered: !!o11yMap[node.id] };
          });
          return { ...infra, node: merged };
        }));
        setAllInfras(enriched);
        setNodes(enriched.flatMap(i => i.node || []));
      }
    } catch { if (!silent) { setNodes([]); setAllInfras([]); } }
    if (!silent) setLoading(false);
  }, [nsId, infraId]);

  useEffect(() => { loadNodes(); return () => { if (pollRef.current) clearInterval(pollRef.current); }; }, [loadNodes]);

  // Silently re-poll agent status so transient states (e.g. SERVICE_INACTIVE right after a
  // VM suspend/resume, before the agent finishes restarting) self-correct without a manual
  // refresh. Skipped while an install/uninstall or metric op is running.
  const loadNodesRef = useRef(loadNodes);
  useEffect(() => { loadNodesRef.current = loadNodes; }, [loadNodes]);
  const mutatingRef = useRef(false);
  useEffect(() => { mutatingRef.current = busy || globalBusy; }, [busy, globalBusy]);
  useEffect(() => {
    const id = setInterval(() => { if (!mutatingRef.current) loadNodesRef.current(true); }, 10000);
    return () => clearInterval(id);
  }, []);
  useEffect(() => { getPlugins().then(setPlugins).catch(() => setPlugins([])); }, []);

  async function selectNode(node) {
    if (globalBusy) return;
    setSelectedNode(node);
    setSelectedNodeInfraId(node.infraId || infraId);
    setItems([]);
    if (!node.registered) return;
    setItemLoading(true);
    try { setItems(await getNodeItems(nsId, node.infraId || infraId, node.id)); } catch { setItems([]); }
    setItemLoading(false);
  }

  // Keep the metric-panel selection in sync with the node's active items.
  useEffect(() => { setPicked(new Set(items.map((it) => it.pluginSeq))); }, [items]);

  // Apply the checkbox selection in one action (K8s-style): enable newly picked,
  // disable newly unpicked.
  async function handleApplyMetrics() {
    if (!selectedNode || busy) return;
    const infra = selectedNodeInfraId || infraId;
    const active = new Set(items.map((it) => it.pluginSeq));
    const toAdd = inputPlugins.filter((p) => picked.has(p.seq) && !active.has(p.seq));
    const toRemove = items.filter((it) => !picked.has(it.pluginSeq));
    if (toAdd.length === 0 && toRemove.length === 0) return;
    setBusy(true); setBusyMsg('Applying metric selection...');
    try {
      for (const it of toRemove) await deleteNodeItem(nsId, infra, selectedNode.id, it.seq);
      for (const p of toAdd) await createNodeItem(nsId, infra, selectedNode.id, { pluginSeq: p.seq });
      setItems(await getNodeItems(nsId, infra, selectedNode.id));
    } catch (err) {
      alert('Failed: ' + (err.response?.data?.error_message || err.message));
    }
    setBusy(false); setBusyMsg('');
  }

  // --- Install / Uninstall with polling ---
  async function handleInstall(e, node) {
    e.stopPropagation();
    if (!confirm(`Install monitoring agent on "${node.name || node.id}"?`)) return;
    setGlobalBusy(true);
    setBusyMsg(`Installing agent on ${node.name || node.id}...`);
    try {
      await installAgent(nsId, node.infraId || infraId, node.id);
      startPolling(node.id);
    } catch (err) {
      alert('Install failed: ' + (err.response?.data?.error_message || err.message));
      setGlobalBusy(false);
      setBusyMsg('');
    }
  }

  async function handleUninstall(e, node) {
    e.stopPropagation();
    if (!confirm(`Uninstall monitoring agent from "${node.name || node.id}"?`)) return;
    setGlobalBusy(true);
    setBusyMsg(`Uninstalling agent from ${node.name || node.id}...`);
    try {
      await uninstallAgent(nsId, node.infraId || infraId, node.id);
      await loadNodes();
      if (selectedNode?.id === node.id) { setSelectedNode(null); setItems([]); }
    } catch (err) {
      alert('Uninstall failed: ' + (err.response?.data?.error_message || err.message));
    }
    setGlobalBusy(false);
    setBusyMsg('');
  }

  function startPolling(nodeId) {
    if (pollRef.current) clearInterval(pollRef.current);
    let count = 0;
    pollRef.current = setInterval(async () => {
      count++;
      try {
        const nodeData = await getNode(nsId, selectedNodeInfraId || infraId, nodeId);
        const status = nodeData?.monitoring_agent_status || nodeData?.monitoringAgentStatus;
        setBusyMsg(`Installing agent... (${status || 'checking'}) [${count * 5}s]`);
        if (status === 'SUCCESS' || status === 'FAILED' || count > 60) {
          clearInterval(pollRef.current);
          pollRef.current = null;
          setGlobalBusy(false);
          setBusyMsg('');
          await loadNodes();
          if (status === 'SUCCESS') {
            // Auto-select the Node to show items
            const refreshedNodes = await getNodeList(nsId, infraId);
            const found = refreshedNodes.find(n => (n.node_id || n.id) === nodeId);
            if (found) selectNode({ ...found, id: nodeId, registered: true });
          }
        }
      } catch {
        // Keep polling
      }
    }, 5000);
  }

  // --- Metric toggle with confirmation + overlay ---
  async function handleToggle(plugin, isActive) {
    if (!selectedNode || busy) return;
    const action = isActive ? 'Disable' : 'Enable';
    if (!confirm(`${action} "${plugin.name}" monitoring metric on ${selectedNode.name || selectedNode.id}?`)) return;

    setBusy(true);
    setBusyMsg(`${action === 'Enable' ? 'Enabling' : 'Disabling'} ${plugin.name} metric...`);
    try {
      if (isActive) {
        const item = items.find((it) => it.pluginSeq === plugin.seq);
        if (item) await deleteNodeItem(nsId, selectedNodeInfraId || infraId, selectedNode.id, item.seq);
      } else {
        await createNodeItem(nsId, selectedNodeInfraId || infraId, selectedNode.id, { pluginSeq: plugin.seq });
      }
      setItems(await getNodeItems(nsId, selectedNodeInfraId || infraId, selectedNode.id));
    } catch (err) {
      alert('Failed: ' + (err.response?.data?.error_message || err.message));
    }
    setBusy(false);
    setBusyMsg('');
  }

  const filteredNodes = filter ? nodes.filter((node) => (node.name || node.id || '').toLowerCase().includes(filter.toLowerCase())) : nodes;
  const inputPlugins = plugins.filter((p) => p.pluginType === 'INPUT');
  const activeSeqs = new Set(items.map((it) => it.pluginSeq));

  return (
    <div className="space-y-4 relative">
      {/* Global busy overlay (install/uninstall) */}
      {globalBusy && (
        <div className="fixed inset-0 bg-white/60 backdrop-blur-sm z-50 flex items-center justify-center">
          <div className="bg-white rounded-lg shadow-xl p-6 text-center">
            <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <p className="text-sm font-medium text-gray-700">{busyMsg}</p>
            <p className="text-xs text-gray-400 mt-1">Please wait...</p>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold">Monitor Setting</div>
        <div className="p-4 flex items-center gap-4">
          <span className="text-sm text-gray-600">{infraId ? 'Workload:' : 'Namespace:'}</span>
          <span className="font-medium">{infraId || nsId}</span>
          <button onClick={loadNodes} className="ml-auto text-sm text-gray-500 hover:text-gray-700">Refresh</button>
        </div>
      </div>

      {/* K8s cluster host-agent management */}
      <K8sAgentPanel nsId={nsId} />

      {/* Server list — grouped by Infra */}
      {loading ? <div className="bg-white rounded-lg shadow p-4 text-sm text-gray-400 animate-pulse">Loading servers & agent status…</div>
      : allInfras.map((infra) => {
        const infraNodes = filter ? (infra.node || []).filter((node) => (node.name || node.id || '').toLowerCase().includes(filter.toLowerCase())) : (infra.node || []);
        return (
        <div key={infra.id} className="bg-white rounded-lg shadow">
          <div className="px-4 py-3 border-b flex items-center gap-3">
            <span className="font-semibold text-sm">{infra.name || infra.id}</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${(infra.status || '').includes('Running') ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{infra.status || '-'}</span>
            <span className="text-xs text-gray-400">{(infra.node || []).length} Nodes</span>
            <input type="text" placeholder="Filter..." value={filter} onChange={(e) => setFilter(e.target.value)} className="ml-auto border rounded px-2 py-1 text-xs w-32" />
          </div>
          <div className="overflow-auto">
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                <th className="px-4 py-2.5 border-b text-gray-500">Name</th>
                <th className="px-4 py-2.5 border-b text-gray-500">Node ID</th>
                <th className="px-4 py-2.5 border-b text-gray-500">Status</th>
                <th className="px-4 py-2.5 border-b text-gray-500">Monitoring Agent</th>
                <th className="px-4 py-2.5 border-b text-gray-500">Log Agent</th>
              </tr></thead>
              <tbody>
                {infraNodes.length === 0 ? <tr><td colSpan={5} className="px-4 py-6 text-center text-gray-400">No servers</td></tr>
                : infraNodes.map((node) => {
                  const run = nodeRunState(node.status);
                  // VM agents (telegraf + fluent-bit) install/uninstall together, so the same
                  // control is shown in BOTH the Monitoring Agent and Log Agent columns.
                  const renderAction = () => (run !== 'running' && !node.registered)
                    ? <span className="text-xs text-gray-400" title="Node is not running; agent state unknown">—</span>
                    : !node.registered
                    ? <button onClick={(e) => handleInstall(e, node)} disabled={busy} className="text-xs bg-blue-600 text-white px-2 py-1 rounded hover:bg-blue-700 disabled:opacity-50">Install</button>
                    : node.monitoring_agent_status === 'INSTALLING'
                    ? <span className="text-xs text-yellow-600 animate-pulse">Installing…</span>
                    : <button onClick={(e) => handleUninstall(e, node)} disabled={busy} className="text-xs text-red-500 hover:text-red-700 disabled:opacity-50">Uninstall</button>;
                  return (
                  <tr key={node.id} onClick={() => selectNode(node)}
                    className={`cursor-pointer hover:bg-blue-50 ${selectedNode?.id === node.id ? 'bg-blue-100' : ''}`}>
                    <td className="px-4 py-2.5 border-b font-medium"><span className="inline-flex items-center gap-1.5"><ProviderBadge connectionName={node.connectionName} showLabel={false} />{node.name || node.id}</span></td>
                    <td className="px-4 py-2.5 border-b text-gray-500">{node.id}</td>
                    <td className="px-4 py-2.5 border-b"><Badge status={node.status} /></td>
                    <td className="px-4 py-2.5 border-b" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center gap-2"><AgentBadge status={node.monitoring_agent_status} run={run} />{renderAction()}</div>
                    </td>
                    <td className="px-4 py-2.5 border-b" onClick={(e) => e.stopPropagation()}>
                      <div className="flex items-center gap-2"><AgentBadge status={node.log_agent_status} run={run} />{renderAction()}</div>
                    </td>
                  </tr>
                  );
                })}
              </tbody>
            </table>
        </div>
        {/* Metric panel renders directly under the group that owns the selected node */}
        {selectedNode && (selectedNode.infraId || selectedNodeInfraId) === infra.id && renderMetricPanel()}
      </div>
      );
      })}
    </div>
  );

  function renderMetricPanel() {
    if (!selectedNode) return null;
    return (
        <div className="bg-white rounded-lg shadow mt-3">
          <div className="px-4 py-3 border-b font-semibold">Monitoring Metrics — {selectedNode.name || selectedNode.id}</div>
          <div className="p-4 relative">
            {/* Metric toggle busy overlay */}
            {busy && (
              <div className="absolute inset-0 bg-white/70 backdrop-blur-[2px] z-10 flex items-center justify-center rounded">
                <div className="text-center">
                  <div className="w-6 h-6 border-3 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
                  <p className="text-sm text-gray-600">{busyMsg}</p>
                </div>
              </div>
            )}
            {!selectedNode.registered ? (
              nodeRunState(selectedNode.status) === 'running' ? (
                <div className="text-center py-6">
                  <p className="text-sm text-gray-500 mb-3">Agent not installed.</p>
                  <button onClick={(e) => handleInstall(e, selectedNode)} disabled={busy} className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50">Install Agent</button>
                </div>
              ) : (
                <div className="text-center py-6 text-sm text-gray-500">
                  {nodeRunState(selectedNode.status) === 'unknown'
                    ? `Agent state unknown (node status: ${selectedNode.status || 'N/A'}).`
                    : `Node is not running (${selectedNode.status}). Start the node before installing the agent.`}
                </div>
              )
            ) : itemLoading ? (
              <p className="text-sm text-gray-400 animate-pulse">Loading metrics...</p>
            ) : (
              <>
                <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2 mb-3">
                  {inputPlugins.map((plugin) => {
                    const on = picked.has(plugin.seq);
                    const wasActive = activeSeqs.has(plugin.seq);
                    return (
                      <label key={plugin.seq} className="flex items-center gap-2 text-sm bg-white border rounded px-3 py-2 cursor-pointer">
                        <input type="checkbox" checked={on} disabled={busy}
                          onChange={() => setPicked((p) => { const n = new Set(p); n.has(plugin.seq) ? n.delete(plugin.seq) : n.add(plugin.seq); return n; })} />
                        <span className="font-medium">{plugin.name}</span>
                        {wasActive && <span className="ml-auto text-xs text-green-600">active</span>}
                      </label>
                    );
                  })}
                </div>
                <button onClick={handleApplyMetrics} disabled={busy}
                  className="text-sm bg-blue-600 text-white px-4 py-1.5 rounded hover:bg-blue-700 disabled:opacity-50">
                  Apply
                </button>
                <span className="ml-3 text-xs text-gray-400">Enables the selected metrics and disables the rest.</span>
              </>
            )}
          </div>
        </div>
    );
  }
}

function Badge({ status }) {
  const s = (status || '').toUpperCase();
  const c = s.includes('RUNNING') ? 'bg-green-100 text-green-700' : s === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-500';
  return <span className={`text-xs px-2 py-0.5 rounded-full ${c}`}>{status || '-'}</span>;
}

function AgentBadge({ status, run }) {
  if (!status) {
    // No agent status: only call it "Not installed" when the node is actually running.
    // For stopped/terminated/undefined nodes we don't know the agent state.
    return <span className="text-xs text-gray-400">{run === 'running' ? 'Not installed' : 'Unknown'}</span>;
  }
  const s = status.toUpperCase();
  const c = s === 'SUCCESS' ? 'bg-green-100 text-green-700' : s === 'INSTALLING' ? 'bg-yellow-100 text-yellow-700' :
    s === 'SERVICE_INACTIVE' ? 'bg-orange-100 text-orange-700' : s === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-500';
  return <span className={`text-xs px-2 py-0.5 rounded-full ${c}`}>{status}</span>;
}
