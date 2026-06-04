import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useBasePath from '../hooks/useBasePath';
import { getInfraList } from '../api/tumblebug';
import { getMetricsByNode } from '../api/monitoring';
import { getAllCspMetrics, CSP_METRICS, isCspSupported } from '../api/csp';
import { getClusters, getCluster, getAllClusterNodeMetrics } from '../api/k8s';
import MetricChart from '../components/MetricChart';
import ProviderBadge from '../components/ProviderBadge';

const AGENT_METRICS = [
  { key: 'cpu', measurement: 'cpu', field: 'usage_idle', label: 'CPU Used', unit: '%', invert: true },
  { key: 'mem', measurement: 'mem', field: 'used_percent', label: 'Memory Used', unit: '%' },
  { key: 'disk', measurement: 'disk', field: 'used_percent', label: 'Disk Used', unit: '%' },
];

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

  // Infra 선택되면 Node 탭, Infra 없으면 유지
  useEffect(() => {
    if (infraId) setViewTab('node');
  }, [infraId]);
  const [clustersLoading, setClustersLoading] = useState(false);
  const [k8sNodeData, setK8sNodeData] = useState({});
  const [k8sNodeMetricsLoading, setK8sNodeMetricsLoading] = useState(false);
  const [hiddenK8sNodeIds, setHiddenK8sNodeIds] = useState(new Set()); // empty = all visible

  useEffect(() => {
    if (!nsId) return;
    loadData();
  }, [nsId, infraId, viewTab]);

  async function loadData() {
    setLoading(true);
    try {
      if (viewTab === 'k8s') {
        // K8s: load all Infras to find connections
        const infras = await getInfraList(nsId);
        setAllInfras(infras);
        setNodes([]);
      } else {
        // Node: load all Infras in NS, show grouped
        const infras = await getInfraList(nsId);
        setAllInfras(infras);
        const allNodes = infras.flatMap(i => i.node || []);
        setNodes(allNodes);
        if (allNodes.length > 0) {
          setMetricsLoading(true);
          if (dataSource === 'agent') await loadAgentData(allNodes, infras);
          else await loadCspData(allNodes);
          setMetricsLoading(false);
        }
      }
    } catch { setNodes([]); setAllInfras([]); }
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
    (async () => {
      // Get all Infras in namespace to find all connections
      let infras = [];
      try { infras = await getInfraList(nsId); } catch {}
      setAllInfras(infras);
      const allNodes = infras.flatMap(i => i.node || []);
      const connNames = [...new Set(allNodes.map(n => n.connectionName).filter(Boolean))];
      // Search clusters across all connections
      const results = await Promise.allSettled(connNames.map(async (conn) => {
        const list = await getClusters(conn);
        const detailed = await Promise.allSettled(list.map(c => getCluster(conn, c.IId?.NameId)));
        return detailed.filter(r => r.status === 'fulfilled').map(r => ({ ...r.value, connectionName: conn }));
      }));
      setClusters(results.filter(r => r.status === 'fulfilled').flatMap(r => r.value));
    })().finally(() => setClustersLoading(false));
  }, [viewTab, nsId]);

  // Group by cluster + NodeGroup, with per-node entries (1-indexed nodeNumber per cb-spider API)
  const k8sGroups = clusters.flatMap((cluster) =>
    (cluster.NodeGroupList || []).map((ng) => ({
      key: `${cluster.connectionName}/${cluster.IId?.NameId}/${ng.IId?.NameId}`,
      cluster,
      nodeGroup: ng,
      nodes: (ng.Nodes || []).map((node, idx) => ({
        id: `${cluster.connectionName}/${cluster.IId?.NameId}/${ng.IId?.NameId}/${idx + 1}`,
        node,
        nodeNumber: idx + 1,
        nodeGroupName: ng.IId?.NameId,
        clusterName: cluster.IId?.NameId,
        clusterStatus: cluster.Status,
        connectionName: cluster.connectionName,
      })),
    }))
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
        if (!isCspSupported(n.connectionName)) return;
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

      {/* K8s Tab */}
      {viewTab === 'k8s' && (
        clustersLoading ? (
          <div className="text-sm text-gray-400 animate-pulse p-4">Loading clusters...</div>
        ) : k8sNodes.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center text-sm text-gray-400">No K8s nodes found</div>
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
                  const nodeName = n.node?.NameId || n.node?.SystemId || `node-${n.nodeNumber}`;
                  return (
                    <label key={n.id} className="flex items-center gap-1.5 text-xs cursor-pointer select-none">
                      <input
                        type="checkbox"
                        className="h-3.5 w-3.5 cursor-pointer accent-blue-600"
                        checked={!hiddenK8sNodeIds.has(n.id)}
                        onChange={() => toggleK8sNode(n.id)}
                      />
                      <span className="font-mono text-gray-600">{nodeName}</span>
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
      {viewTab === 'node' && (infraId ? allInfras.filter(i => i.id === infraId || i.name === infraId) : allInfras).map((infra) => (
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
      ))}
    </div>
  );
}

function K8sNodeCard({ info, metrics, dataSource, metricsLoading, selectedChart, onSelectChart, onClickChart }) {
  const cspSupported = isCspSupported(info.connectionName);
  const nodeName = info.node?.NameId || info.node?.SystemId || `node-${info.nodeNumber}`;

  const header = (
    <div className="flex items-center justify-between px-4 py-2.5 border-b bg-white">
      <div className="flex items-center gap-2 min-w-0">
        <span className="text-[10px] uppercase tracking-wide text-gray-400 font-medium">Node</span>
        <span className="font-semibold text-sm truncate font-mono">{nodeName}</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${info.clusterStatus === 'Active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{info.clusterStatus || '-'}</span>
        {dataSource === 'csp' && cspSupported && <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-600" title="CSP API Based">API</span>}
      </div>
      <div className="text-xs text-gray-400 shrink-0">
        <span className="font-mono">#{info.nodeNumber}</span>
      </div>
    </div>
  );

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
  const gauges = AGENT_METRICS.map((m) => {
    const d = metrics[m.key];
    const last = d?.res ? getLastValue(d.res) : null;
    const val = last != null ? (m.invert ? 100 - last : last) : null;
    return { ...m, value: val, display: val != null ? val.toFixed(1) + '%' : '-' };
  });

  const activeMetric = AGENT_METRICS.find((m) => m.key === selectedChart) || AGENT_METRICS[0];
  const chartData = metrics[selectedChart];
  const chartSeries = chartData?.res ? toSeries(chartData.res, activeMetric.label, activeMetric.invert) : [];

  return (
    <div className="bg-white rounded-lg shadow">
      <NodeHeader node={node} showCspBadge={false} cspAvailable={cspSupported} />
      <div className="flex">
        <div className="flex flex-col justify-center gap-2 px-4 py-3 w-52 shrink-0 border-r">
          {gauges.map((g) => (
            <GaugeItem key={g.key} g={g} active={selectedChart === g.key} onClick={() => onSelectChart(g.key)} />
          ))}
        </div>
        <div className="flex-1 px-2 py-1 min-w-0 cursor-pointer" onClick={onClickChart}>
          {metricsLoading ? (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm animate-pulse">Loading Agent data...</div>
          ) : (
            <MetricChart title={activeMetric.label} series={chartSeries} height={220} measurement={activeMetric.measurement} metric={activeMetric.field} />
          )}
        </div>
      </div>
    </div>
  );
}

function CspNodeCard({ node, metrics, metricsLoading, selectedChart, onSelectChart, onClickChart, cspSupported }) {
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
