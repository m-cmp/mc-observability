import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useBasePath from '../hooks/useBasePath';
import { getInfraList } from '../api/tumblebug';
import { getMetricsByNode } from '../api/monitoring';
import { getAllCspMetrics, CSP_METRICS, isCspSupported } from '../api/csp';
import { getClustersDetailed, getAllClusterNodeMetrics } from '../api/k8s';
import { getNodeList } from '../api/node';
import { getK8sClusters } from '../api/k8sAgent';
import MetricChart from '../components/MetricChart';
import ProviderBadge from '../components/ProviderBadge';
import AgentNotInstalled from '../components/AgentNotInstalled';
import K8sAgentMonitor from '../components/K8sAgentMonitor';

// Mirrors the full telegraf input set the host agent collects (cpu/mem/disk/diskio/
// net/system/processes/swap) so the Agent tab shows varied graphs like the API tab.
const AGENT_METRICS = [
  { key: 'cpu', measurement: 'cpu', field: 'usage_idle', label: 'CPU Used', unit: '%', invert: true },
  { key: 'cpu_user', measurement: 'cpu', field: 'usage_user', label: 'CPU User', unit: '%' },
  { key: 'cpu_system', measurement: 'cpu', field: 'usage_system', label: 'CPU System', unit: '%' },
  { key: 'cpu_iowait', measurement: 'cpu', field: 'usage_iowait', label: 'CPU IOWait', unit: '%' },
  { key: 'mem', measurement: 'mem', field: 'used_percent', label: 'Memory Used', unit: '%' },
  { key: 'swap', measurement: 'swap', field: 'used_percent', label: 'Swap Used', unit: '%' },
  { key: 'disk', measurement: 'disk', field: 'used_percent', label: 'Disk Used', unit: '%' },
  { key: 'diskio_read', measurement: 'diskio', field: 'read_bytes', label: 'Disk Read', unit: 'bytes' },
  { key: 'diskio_write', measurement: 'diskio', field: 'write_bytes', label: 'Disk Write', unit: 'bytes' },
  { key: 'net_recv', measurement: 'net', field: 'bytes_recv', label: 'Net Recv', unit: 'bytes' },
  { key: 'net_sent', measurement: 'net', field: 'bytes_sent', label: 'Net Sent', unit: 'bytes' },
  { key: 'load1', measurement: 'system', field: 'load1', label: 'Load (1m)', unit: '' },
];

function fmtAgentValue(v, unit) {
  if (v == null) return '-';
  if (unit === '%') return v.toFixed(1) + '%';
  if (unit === 'bytes') return formatCspValue(v, 'bytes');
  return Number.isInteger(v) ? String(v) : v.toFixed(2);
}

// A VM/node host is "powered on" when its (cb-tumblebug) status is Running. Anything else
// (Suspended/Suspending/Terminated/…) means the host is down, so — like stopped K8s nodes —
// we show a "powered off" state instead of empty charts.
function isPowered(status) {
  return String(status || '').toLowerCase().includes('running');
}

export default function InfraOverview() {
  const { nsId, infraId } = useParams();
  const navigate = useNavigate();
  const base = useBasePath();
  const [nodes, setNodes] = useState([]);
  const [allInfras, setAllInfras] = useState([]); // NS-level: all Infras with Nodes
  const [nodeData, setNodeData] = useState({});
  const [loading, setLoading] = useState(true);
  const [metricsLoading, setMetricsLoading] = useState(false);
  const [selectedChart, setSelectedChart] = useState('cpu');
  const [dataSource, setDataSource] = useState('agent');
  const [viewTab, setViewTab] = useState(infraId ? 'node' : 'node');
  const [clusters, setClusters] = useState([]);

  // Switch to Node tab when an Infra is selected; keep otherwise
  useEffect(() => {
    if (infraId) setViewTab('node');
  }, [infraId]);
  const [clustersLoading, setClustersLoading] = useState(false);
  const [k8sNodeData, setK8sNodeData] = useState({});
  const [k8sNodeMetricsLoading, setK8sNodeMetricsLoading] = useState(false);
  const [hiddenK8sNodeIds, setHiddenK8sNodeIds] = useState(new Set()); // empty = all visible
  // Distinguishes "backend unreachable" from "genuinely empty" so a failed fetch
  // surfaces an error + Retry instead of a misleading "No data" / "No nodes" state.
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const retry = () => setReloadKey((k) => k + 1);

  useEffect(() => {
    if (!nsId) return;
    loadData();
  }, [nsId, infraId, viewTab, reloadKey]);

  async function loadData() {
    setLoading(true);
    setError(null);
    try {
      if (viewTab === 'k8s') {
        // K8s: load all Infras to find connections
        const infras = await getInfraList(nsId);
        setAllInfras(infras);
        setNodes([]);
      } else {
        // Node: load all Infras in NS, show grouped
        const infras = await getInfraList(nsId);
        // Enrich each Node with its Infra id + whether the monitoring agent is
        // installed (present in the o11y node list). Used to show an install
        // hint instead of an empty "No data" chart.
        const enriched = await Promise.all((infras || []).map(async (infra) => {
          let o11yNodes = [];
          try { o11yNodes = await getNodeList(nsId, infra.id); } catch {}
          const reg = new Set(o11yNodes.map((n) => n.node_id || n.id));
          return { ...infra, node: (infra.node || []).map((n) => ({ ...n, infraId: infra.id, registered: reg.has(n.id) })) };
        }));
        setAllInfras(enriched);
        const allNodes = enriched.flatMap(i => i.node || []);
        setNodes(allNodes);
        if (allNodes.length > 0) {
          setMetricsLoading(true);
          if (dataSource === 'agent') await loadAgentData(allNodes, enriched);
          else await loadCspData(allNodes);
          setMetricsLoading(false);
        }
      }
    } catch (e) {
      // getInfraList (the primary discovery call) threw → backend unreachable, not empty.
      setNodes([]); setAllInfras([]);
      setError(e?.message || 'Failed to load data from the monitoring backend.');
    }
    setLoading(false);
  }

  // Reload when dataSource changes
  useEffect(() => {
    if (nodes.length === 0) return;
    setNodeData({});
    setMetricsLoading(true);
    const load = dataSource === 'agent' ? loadAgentData : loadCspData;
    load(nodes).finally(() => setMetricsLoading(false));
  }, [dataSource]);

  async function loadAgentData(nodeList, infrasArg) {
    // Find which Infra each Node belongs to. Caller must pass the current infras
    // since the allInfras state may still be stale (setState is async).
    const infras = infrasArg || allInfras;
    const nodeInfraMap = {};
    infras.forEach(i => (i.node || []).forEach(n => { nodeInfraMap[n.id] = i.id; }));

    nodeList.forEach(async (node) => {
      const nodeInfraId = nodeInfraMap[node.id] || infraId;
      await Promise.allSettled(
        AGENT_METRICS.map(async (m) => {
          try {
            const res = await getMetricsByNode(nsId, nodeInfraId, node.id, {
              measurement: m.measurement, range: '1h', groupTime: '1m',
              fields: [{ function: 'mean', field: m.field }],
            });
            setNodeData(prev => ({ ...prev, [node.id]: { ...(prev[node.id] || {}), [m.key]: { res, ...m } } }));
          } catch {}
        })
      );
    });
  }

  async function loadCspData(nodeList) {
    nodeList.forEach(async (node) => {
      if (!node.connectionName || !node.cspResourceName) return;
      const cspData = await getAllCspMetrics(node.connectionName, node.cspResourceName, '1');
      setNodeData(prev => ({ ...prev, [node.id]: cspData }));
    });
  }

  // Load all Infras in NS + K8s clusters when K8s tab switches
  useEffect(() => {
    if (viewTab !== 'k8s') return;
    setClustersLoading(true);
    setError(null);
    (async () => {
      // Get all Infras in namespace to find all connections
      let infras = [];
      let discoveryFailed = false;
      try { infras = await getInfraList(nsId); } catch { discoveryFailed = true; }
      setAllInfras(infras);
      // Connections to search: from the namespace's K8s clusters (Tumblebug) primarily —
      // VMs may not exist — unioned with any VM-derived connections.
      let tbConns = [];
      try { tbConns = (await getK8sClusters(nsId)).map((c) => c.connectionName).filter(Boolean); } catch { discoveryFailed = true; }
      const vmConns = infras.flatMap(i => i.node || []).map(n => n.connectionName).filter(Boolean);
      const connNames = [...new Set([...tbConns, ...vmConns])];
      // Search clusters across all connections (cached, single call per connection).
      const results = await Promise.allSettled(connNames.map(async (conn) => {
        const detailed = await getClustersDetailed(conn);
        return detailed.map(c => ({ ...c, connectionName: conn }));
      }));
      const ok = results.filter(r => r.status === 'fulfilled');
      // Surface a connection error (vs. a genuinely empty NS) when discovery calls
      // threw and yielded nothing, or when every cluster-detail fetch failed.
      if (discoveryFailed && connNames.length === 0) {
        setError('Failed to load K8s clusters from the monitoring backend.');
      } else if (connNames.length > 0 && ok.length === 0) {
        setError('Failed to load K8s cluster details from the monitoring backend.');
      }
      setClusters(ok.flatMap(r => r.value));
    })().finally(() => setClustersLoading(false));
  }, [viewTab, nsId, reloadKey]);

  // Group by cluster + NodeGroup, with per-node entries (1-indexed nodeNumber per cb-spider API).
  // When the cluster is powered off, cb-spider returns NodeGroup with Nodes == null but keeps
  // DesiredNodeSize — synthesize that many "stopped" placeholder nodes so the node count and
  // per-node status still show (real names aren't retrievable while the cluster is off).
  const k8sGroups = clusters.flatMap((cluster) =>
    (cluster.NodeGroupList || []).map((ng) => {
      const real = (ng.Nodes || []).map((node, idx) => ({
        id: `${cluster.connectionName}/${cluster.IId?.NameId}/${ng.IId?.NameId}/${idx + 1}`,
        node,
        nodeNumber: idx + 1,
        powered: true,
        placeholder: false,
        nodeGroupName: ng.IId?.NameId,
        clusterName: cluster.IId?.NameId,
        clusterStatus: cluster.Status,
        connectionName: cluster.connectionName,
      }));
      const stoppedCount = Math.max(0, (ng.DesiredNodeSize || 0) - real.length);
      const stopped = Array.from({ length: stoppedCount }, (_, i) => {
        const num = real.length + i + 1;
        return {
          id: `${cluster.connectionName}/${cluster.IId?.NameId}/${ng.IId?.NameId}/off-${num}`,
          node: null,
          nodeNumber: num,
          powered: false,
          placeholder: true,
          nodeGroupName: ng.IId?.NameId,
          clusterName: cluster.IId?.NameId,
          clusterStatus: cluster.Status,
          connectionName: cluster.connectionName,
        };
      });
      return {
        key: `${cluster.connectionName}/${cluster.IId?.NameId}/${ng.IId?.NameId}`,
        cluster,
        nodeGroup: ng,
        nodes: [...real, ...stopped],
      };
    })
  );
  const k8sNodes = k8sGroups.flatMap((g) => g.nodes);

  function toggleK8sNode(id) {
    setHiddenK8sNodeIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }
  function toggleK8sGroup(ids, show) {
    setHiddenK8sNodeIds((prev) => {
      const next = new Set(prev);
      if (show) ids.forEach((id) => next.delete(id));
      else ids.forEach((id) => next.add(id));
      return next;
    });
  }

  // Load CSP (API) metrics for each K8s node when K8s tab is active + API mode
  useEffect(() => {
    if (viewTab !== 'k8s' || dataSource !== 'csp' || k8sNodes.length === 0) {
      setK8sNodeData({});
      return;
    }
    setK8sNodeMetricsLoading(true);
    const data = {};
    Promise.allSettled(
      k8sNodes.map(async (n) => {
        if (n.placeholder || !isCspSupported(n.connectionName)) return;
        const m = await getAllClusterNodeMetrics(n.connectionName, n.clusterName, n.nodeGroupName, String(n.nodeNumber), '1');
        data[n.id] = m;
      })
    ).then(() => {
      setK8sNodeData({ ...data });
    }).finally(() => setK8sNodeMetricsLoading(false));
  }, [viewTab, dataSource, clusters]);

  if (loading) return <p className="text-sm text-gray-400 p-4">Loading...</p>;

  const allNodesFlat = allInfras.flatMap(i => i.node || []);
  const hasCspNode = allNodesFlat.some((node) => isCspSupported(node.connectionName));
  const showDataSourceToggle = (viewTab === 'node' && hasCspNode) || viewTab === 'k8s';
  const totalNodes = allNodesFlat.length;

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h2 className="text-lg font-semibold">{viewTab === 'k8s' || !infraId ? `Namespace — ${nsId}` : `Infra Overview — ${infraId}`}</h2>
          {/* Node / K8s tab */}
          <div className="flex bg-gray-100 rounded-lg p-0.5 text-xs">
            <button onClick={() => { setViewTab('node'); if (infraId) navigate(`${base}/monitoring/${nsId}`); }}
              className={`px-3 py-1.5 rounded-md ${viewTab === 'node' ? 'bg-white shadow text-gray-800 font-medium' : 'text-gray-500'}`}>
              Node
            </button>
            <button onClick={() => { setViewTab('k8s'); navigate(`${base}/monitoring/${nsId}`); }}
              className={`px-3 py-1.5 rounded-md ${viewTab === 'k8s' ? 'bg-white shadow text-gray-800 font-medium' : 'text-gray-500'}`}>
              K8s
            </button>
          </div>
        </div>
        <div className="flex items-center gap-3">
          {showDataSourceToggle && (
            <div className="flex bg-gray-100 rounded-lg p-0.5 text-xs">
              <button onClick={() => setDataSource('agent')}
                className={`px-3 py-1 rounded-md ${dataSource === 'agent' ? 'bg-white shadow text-blue-600 font-medium' : 'text-gray-500'}`}>
                Agent
              </button>
              <button onClick={() => setDataSource('csp')} title="CSP API Based"
                className={`px-3 py-1 rounded-md ${dataSource === 'csp' ? 'bg-white shadow text-blue-600 font-medium' : 'text-gray-500'}`}>
                API
              </button>
            </div>
          )}
          <span className="text-xs text-gray-400">{viewTab === 'node' ? `${totalNodes} Nodes / ${allInfras.length} Infras` : `${k8sNodes.length} Nodes / ${clusters.length} Clusters`}</span>
        </div>
      </div>

      {/* Backend unreachable → show an explicit error + Retry instead of a silent empty state. */}
      {error && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center justify-between gap-3">
          <div className="flex items-start gap-2 text-sm text-red-700 min-w-0">
            <span aria-hidden className="mt-0.5">⚠</span>
            <span className="min-w-0">
              Couldn't reach the monitoring backend, so data may be missing or incomplete.
              <span className="block text-xs text-red-500 mt-0.5 break-words">{error}</span>
            </span>
          </div>
          <button onClick={retry}
            className="text-xs px-3 py-1.5 rounded-md bg-red-600 text-white hover:bg-red-700 shrink-0">
            Retry
          </button>
        </div>
      )}

      {/* K8s Tab */}
      {/* K8s tab + Agent source → host telegraf metrics (InfluxDB, by clusterId/nodeName) */}
      {viewTab === 'k8s' && dataSource === 'agent' && <K8sAgentMonitor nsId={nsId} />}

      {/* K8s tab + CSP source → cb-spider API metrics */}
      {viewTab === 'k8s' && dataSource === 'csp' && (
        clustersLoading ? (
          <div className="text-sm text-gray-400 animate-pulse p-4">Loading clusters...</div>
        ) : k8sNodes.length === 0 ? (
          error ? null : <div className="bg-white rounded-lg shadow p-8 text-center text-sm text-gray-400">No K8s nodes found</div>
        ) : k8sGroups.map((g) => {
          const ids = g.nodes.map((n) => n.id);
          const allVisible = ids.length > 0 && ids.every((id) => !hiddenK8sNodeIds.has(id));
          const anyVisible = ids.some((id) => !hiddenK8sNodeIds.has(id));
          const visibleNodes = g.nodes.filter((n) => !hiddenK8sNodeIds.has(n.id));
          return (
            <div key={g.key} className="bg-white rounded-lg shadow">
              {/* Group header */}
              <div className="px-4 py-3 border-b flex items-center gap-3 min-w-0 flex-wrap">
                <ProviderBadge connectionName={g.cluster.connectionName} />
                <div className="flex items-center gap-1.5 min-w-0">
                  <span className="text-[10px] uppercase tracking-wide text-gray-400 font-medium">Cluster</span>
                  <span className="font-semibold text-sm truncate">{g.cluster.IId?.NameId}</span>
                </div>
                <span className="text-gray-300">/</span>
                <div className="flex items-center gap-1.5 min-w-0">
                  <span className="text-[10px] uppercase tracking-wide text-gray-400 font-medium">NodeGroup</span>
                  <span className="font-medium text-sm truncate">{g.nodeGroup.IId?.NameId}</span>
                </div>
                <span className={`text-xs px-2 py-0.5 rounded-full ${g.cluster.Status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{g.cluster.Status || '-'}</span>
                <span className="text-xs text-gray-400 ml-auto">{g.nodes.length} nodes</span>
              </div>
              {/* Left-aligned checkbox strip (nodes) */}
              <div className="px-4 py-2 border-b bg-white flex items-center gap-4 flex-wrap">
                <span className="text-[10px] uppercase tracking-wide text-gray-400 font-medium">Nodes</span>
                <label className="flex items-center gap-1.5 text-xs cursor-pointer select-none">
                  <input
                    type="checkbox"
                    className="h-3.5 w-3.5 cursor-pointer accent-blue-600"
                    checked={allVisible}
                    ref={(el) => { if (el) el.indeterminate = !allVisible && anyVisible; }}
                    onChange={(e) => toggleK8sGroup(ids, e.target.checked)}
                  />
                  <span className="font-semibold text-gray-700">All</span>
                </label>
                {g.nodes.map((n) => {
                  const nodeName = n.node?.NameId || n.node?.SystemId || `${n.nodeGroupName} #${n.nodeNumber}`;
                  return (
                    <label key={n.id} className="flex items-center gap-1.5 text-xs cursor-pointer select-none">
                      <input
                        type="checkbox"
                        className="h-3.5 w-3.5 cursor-pointer accent-blue-600"
                        checked={!hiddenK8sNodeIds.has(n.id)}
                        onChange={() => toggleK8sNode(n.id)}
                      />
                      <span className={`font-mono ${n.placeholder ? 'text-gray-400 italic' : 'text-gray-600'}`}>{nodeName}</span>
                    </label>
                  );
                })}
              </div>
              {/* Nested node cards */}
              {visibleNodes.length === 0 ? (
                <div className="p-6 text-center text-xs text-gray-400">No nodes selected</div>
              ) : (
                <div className="p-3 space-y-3 bg-white">
                  {visibleNodes.map((n) => (
                    <K8sNodeCard
                      key={n.id}
                      info={n}
                      metrics={k8sNodeData[n.id] || {}}
                      dataSource={dataSource}
                      metricsLoading={k8sNodeMetricsLoading}
                      selectedChart={selectedChart}
                      onSelectChart={setSelectedChart}
                      onClickChart={() => navigate(`${base}/monitoring/${nsId}/k8s/${n.connectionName}/${n.clusterName}/${n.nodeGroupName}/${n.nodeNumber}`)}
                    />
                  ))}
                </div>
              )}
            </div>
          );
        })
      )}

      {/* Node Tab — grouped by Infra. When a specific infraId is selected via the
          URL/dropdown, narrow to that one; otherwise show every Infra in the NS. */}
      {viewTab === 'node' && (() => {
        const list = infraId ? allInfras.filter(i => i.id === infraId || i.name === infraId) : allInfras;
        if (list.length === 0) {
          if (error) return null;
          return <div className="bg-white rounded-lg shadow p-8 text-center text-sm text-gray-400">No nodes in this namespace</div>;
        }
        return list.map((infra) => (
        <div key={infra.id} className="bg-white rounded-lg shadow">
          {/* Infra group header */}
          <div className="px-4 py-3 border-b flex items-center gap-3">
            <span className="font-semibold text-sm">{infra.name || infra.id}</span>
            <span className={`text-xs px-2 py-0.5 rounded-full ${(infra.status || '').includes('Running') ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
              {infra.status || '-'}
            </span>
            <span className="text-xs text-gray-400 ml-auto">{(infra.node || []).length} Nodes</span>
          </div>
          {/* Node cards inside */}
          <div className="p-3 space-y-3">
            {(infra.node || []).map((node) => (
              <NodeCard
                key={node.id}
                node={node}
                nodeMetrics={nodeData[node.id] || {}}
                dataSource={dataSource}
                metricsLoading={metricsLoading}
                selectedChart={selectedChart}
                onSelectChart={setSelectedChart}
                onClickChart={() => navigate(`${base}/monitoring/${nsId}/${infra.id}/${node.id}${dataSource === 'csp' ? '?source=csp' : ''}`)}
              />
            ))}
          </div>
        </div>
        ));
      })()}
    </div>
  );
}

function K8sNodeCard({ info, metrics, dataSource, metricsLoading, selectedChart, onSelectChart, onClickChart }) {
  const cspSupported = isCspSupported(info.connectionName);
  const nodeName = info.node?.NameId || info.node?.SystemId || `${info.nodeGroupName} #${info.nodeNumber}`;
  const powered = info.powered !== false;

  const header = (
    <div className="flex items-center justify-between px-4 py-2.5 border-b bg-white">
      <div className="flex items-center gap-2 min-w-0">
        <span className="text-[10px] uppercase tracking-wide text-gray-400 font-medium">Node</span>
        <span className={`font-semibold text-sm truncate font-mono ${info.placeholder ? 'text-gray-400 italic' : ''}`}>{nodeName}</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${powered ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{powered ? 'Running' : 'Stopped'}</span>
        {dataSource === 'csp' && cspSupported && powered && <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-600" title="CSP API Based">API</span>}
      </div>
      <div className="text-xs text-gray-400 shrink-0">
        <span className="font-mono">#{info.nodeNumber}</span>
      </div>
    </div>
  );

  if (!powered) {
    return (
      <div className="bg-white rounded-md border border-gray-200">
        {header}
        <div className="p-8 text-center text-sm text-gray-400">Node is powered off — start the cluster to collect metrics.</div>
      </div>
    );
  }

  if (dataSource === 'agent') {
    return (
      <div className="bg-white rounded-md border border-gray-200">
        {header}
        <div className="p-8 text-center text-sm text-gray-400">Agent monitoring for K8s nodes is not yet supported</div>
      </div>
    );
  }

  if (!cspSupported) {
    return (
      <div className="bg-white rounded-md border border-gray-200">
        {header}
        <div className="p-8 text-center text-sm text-gray-400">API monitoring not supported for this provider</div>
      </div>
    );
  }

  const cspKeys = CSP_METRICS.map((m) => m.key).filter((k) => metrics[k]);
  const activeKey = cspKeys.includes(selectedChart) ? selectedChart : (cspKeys[0] || 'cpu_usage');
  const activeData = metrics[activeKey];

  return (
    <div className="bg-white rounded-md border border-gray-200">
      {header}
      <div className="flex">
        <div className="flex flex-col justify-center gap-2 px-4 py-3 w-52 shrink-0 border-r">
          {cspKeys.map((key) => {
            const m = metrics[key];
            const lastVal = m?.series?.[0]?.data?.length > 0 ? m.series[0].data[m.series[0].data.length - 1].y : null;
            const display = lastVal != null ? formatCspValue(lastVal, m.metricUnit) : '-';
            return (
              <GaugeItem key={key} g={{ key, label: m?.metricName || key, display }}
                active={activeKey === key} onClick={() => onSelectChart(key)} noBar />
            );
          })}
        </div>
        <div className="flex-1 px-2 py-1 min-w-0 cursor-pointer" onClick={onClickChart}>
          {metricsLoading ? (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm animate-pulse">Loading CSP API data...</div>
          ) : activeData?.series ? (
            <MetricChart title={`${activeData.metricName} (${activeData.metricUnit})`} series={activeData.series} height={220} />
          ) : (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm">No data</div>
          )}
        </div>
      </div>
    </div>
  );
}

function NodeCard({ node, nodeMetrics, dataSource, metricsLoading, selectedChart, onSelectChart, onClickChart }) {
  const cspSupported = isCspSupported(node.connectionName);

  if (dataSource === 'csp') {
    return <CspNodeCard node={node} metrics={nodeMetrics} metricsLoading={metricsLoading} selectedChart={selectedChart} onSelectChart={onSelectChart} onClickChart={onClickChart} cspSupported={cspSupported} />;
  }
  return <AgentNodeCard node={node} metrics={nodeMetrics} metricsLoading={metricsLoading} selectedChart={selectedChart} onSelectChart={onSelectChart} onClickChart={onClickChart} cspSupported={cspSupported} />;
}

function AgentNodeCard({ node, metrics, metricsLoading, selectedChart, onSelectChart, onClickChart, cspSupported }) {
  const { nsId } = useParams();

  // Powered off → nothing to collect; show the stopped state (same as stopped K8s nodes).
  if (!isPowered(node.status)) {
    return (
      <div className="bg-white rounded-lg shadow">
        <NodeHeader node={node} showCspBadge={false} cspAvailable={cspSupported} />
        <div className="p-8 text-center text-sm text-gray-400">Node is powered off — start the VM to collect metrics.</div>
      </div>
    );
  }

  const gauges = AGENT_METRICS.map((m) => {
    const d = metrics[m.key];
    const last = d?.res ? getLastValue(d.res) : null;
    const val = last != null ? (m.invert ? 100 - last : last) : null;
    return { ...m, value: m.unit === '%' ? val : null, display: fmtAgentValue(val, m.unit) };
  });

  const activeMetric = AGENT_METRICS.find((m) => m.key === selectedChart) || AGENT_METRICS[0];
  const chartData = metrics[selectedChart];
  const chartSeries = chartData?.res ? toSeries(chartData.res, activeMetric.label, activeMetric.invert) : [];

  // Agent not installed → guide to Config instead of rendering empty charts.
  if (node.registered === false) {
    return (
      <div className="bg-white rounded-lg shadow">
        <NodeHeader node={node} showCspBadge={false} cspAvailable={cspSupported} />
        <AgentNotInstalled nsId={nsId} infraId={node.infraId} nodeId={node.id} height={180}
          nodeStatus={node.status} />
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow">
      <NodeHeader node={node} showCspBadge={false} cspAvailable={cspSupported} />
      <div className="flex">
        <div className="flex flex-col justify-start gap-2 px-4 py-3 w-52 shrink-0 border-r max-h-[380px] overflow-auto">
          {gauges.map((g) => (
            <GaugeItem key={g.key} g={g} active={selectedChart === g.key} onClick={() => onSelectChart(g.key)} noBar={g.unit !== '%'} />
          ))}
        </div>
        <div className="flex-1 px-2 py-1 min-w-0 cursor-pointer" onClick={onClickChart}>
          {metricsLoading ? (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm animate-pulse">Loading Agent data...</div>
          ) : (
            <MetricChart title={`${activeMetric.label}${activeMetric.unit && activeMetric.unit !== '%' ? ` (${activeMetric.unit})` : ''}`} series={chartSeries} height={220} measurement={activeMetric.measurement} metric={activeMetric.field} />
          )}
        </div>
      </div>
    </div>
  );
}

function CspNodeCard({ node, metrics, metricsLoading, selectedChart, onSelectChart, onClickChart, cspSupported }) {
  if (!isPowered(node.status)) {
    return (
      <div className="bg-white rounded-lg shadow">
        <NodeHeader node={node} showCspBadge={cspSupported} cspAvailable={cspSupported} />
        <div className="p-8 text-center text-sm text-gray-400">Node is powered off — start the VM to collect metrics.</div>
      </div>
    );
  }

  if (!cspSupported) {
    return (
      <div className="bg-white rounded-lg shadow">
        <NodeHeader node={node} showCspBadge={true} cspAvailable={false} />
        <div className="p-8 text-center text-sm text-gray-400">API monitoring not supported for this provider</div>
      </div>
    );
  }

  // Maintain consistent order matching CSP_METRICS definition
  const cspKeys = CSP_METRICS.map(m => m.key).filter(k => metrics[k]);
  const activeKey = cspKeys.includes(selectedChart) ? selectedChart : (cspKeys[0] || 'cpu_usage');
  const activeData = metrics[activeKey];

  return (
    <div className="bg-white rounded-lg shadow">
      <NodeHeader node={node} showCspBadge={true} cspAvailable={true} />
      <div className="flex">
        <div className="flex flex-col justify-center gap-2 px-4 py-3 w-52 shrink-0 border-r">
          {cspKeys.map((key) => {
            const m = metrics[key];
            const lastVal = m?.series?.[0]?.data?.length > 0 ? m.series[0].data[m.series[0].data.length - 1].y : null;
            const display = lastVal != null ? formatCspValue(lastVal, m.metricUnit) : '-';
            return (
              <GaugeItem key={key} g={{ key, label: m?.metricName || key, display }}
                active={activeKey === key} onClick={() => onSelectChart(key)} noBar />
            );
          })}
        </div>
        <div className="flex-1 px-2 py-1 min-w-0 cursor-pointer" onClick={onClickChart}>
          {metricsLoading ? (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm animate-pulse">Loading CSP API data...</div>
          ) : activeData?.series ? (
            <MetricChart title={`${activeData.metricName} (${activeData.metricUnit})`} series={activeData.series} height={220} />
          ) : (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm">No data</div>
          )}
        </div>
      </div>
    </div>
  );
}

function NodeHeader({ node, showCspBadge, cspAvailable }) {
  return (
    <div className="flex items-center justify-between px-4 py-3 border-b">
      <div className="flex items-center gap-2">
        <ProviderBadge connectionName={node.connectionName} />
        <span className="font-semibold text-sm">{node.name || node.id}</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${node.status === 'Running' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{node.status || '-'}</span>
        {showCspBadge && cspAvailable && <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-600" title="CSP API Based">API</span>}
        {cspAvailable && !showCspBadge && <span className="text-xs text-gray-400" title="CSP API Based">(API available)</span>}
      </div>
      <div className="text-xs text-gray-400">{node.publicIP && <span className="font-mono">{node.publicIP}</span>}</div>
    </div>
  );
}

function GaugeItem({ g, active, onClick, noBar }) {
  return (
    <div onClick={onClick}
      className={`cursor-pointer rounded-md px-2 py-1.5 transition-colors ${active ? 'bg-blue-50 ring-1 ring-blue-300' : 'hover:bg-gray-50'}`}>
      <div className="flex justify-between text-xs mb-1">
        <span className={`font-medium ${active ? 'text-blue-700' : 'text-gray-500'}`}>{g.label}</span>
        <span className={`font-semibold ${getColor(g.value)}`}>{g.display}</span>
      </div>
      {!noBar && g.value != null && (
        <div className="w-full h-1.5 bg-gray-100 rounded-full overflow-hidden">
          <div className={`h-full rounded-full ${getBarColor(g.value)}`} style={{ width: `${Math.min(100, g.value)}%` }} />
        </div>
      )}
    </div>
  );
}

function formatCspValue(v, unit) {
  if (v == null) return '-';
  const u = (unit || '').toLowerCase();
  if (u.includes('percent') || u === '%') return v.toFixed(1) + '%';
  if (u.includes('bytes')) {
    if (v >= 1e9) return (v / 1e9).toFixed(2) + ' GB';
    if (v >= 1e6) return (v / 1e6).toFixed(2) + ' MB';
    if (v >= 1e3) return (v / 1e3).toFixed(1) + ' KB';
    return v.toFixed(0) + ' B';
  }
  return v.toFixed(2);
}

function getLastValue(metricDTOs) {
  if (!metricDTOs || metricDTOs.length === 0) return null;
  const m = metricDTOs[0];
  if (!m.values || m.values.length === 0) return null;
  const timeIdx = (m.columns || []).indexOf('timestamp');
  const tIdx = timeIdx < 0 ? 0 : timeIdx;
  const valIdx = tIdx === 0 ? 1 : 0;
  // Server returns rows `order by time desc`, so values[0] is newest. Scan for
  // the row with the max timestamp that has a non-null value — handles sparse
  // series where the most recent bucket might still be null.
  let bestTs = -Infinity;
  let bestVal = null;
  for (const row of m.values) {
    if (row[valIdx] == null) continue;
    const raw = row[tIdx];
    const ts = typeof raw === 'string' ? new Date(raw).getTime() : Number(raw);
    if (ts > bestTs) {
      bestTs = ts;
      bestVal = parseFloat(row[valIdx]);
    }
  }
  return bestVal;
}

function toSeries(metricDTOs, name, invert) {
  if (!metricDTOs || metricDTOs.length === 0) return [];
  return metricDTOs.map((m) => {
    const timeIdx = (m.columns || []).indexOf('timestamp');
    const valIdx = timeIdx === 0 ? 1 : 0;
    return {
      name: name || 'value',
      data: (m.values || []).filter((row) => row[valIdx] != null).map((row) => ({
        x: typeof row[timeIdx] === 'string' ? new Date(row[timeIdx]).getTime() : row[timeIdx],
        y: invert ? 100 - parseFloat(row[valIdx]) : parseFloat(row[valIdx]),
      })),
    };
  });
}

function getColor(v) {
  if (v == null) return 'text-gray-400';
  if (v >= 80) return 'text-red-600';
  if (v >= 60) return 'text-yellow-600';
  return 'text-green-600';
}

function getBarColor(v) {
  if (v == null) return 'bg-gray-300';
  if (v >= 80) return 'bg-red-500';
  if (v >= 60) return 'bg-yellow-500';
  return 'bg-blue-500';
}
