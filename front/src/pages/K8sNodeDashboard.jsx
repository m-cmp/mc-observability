import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getClusters, getCluster, getAllClusterNodeMetrics } from '../api/k8s';
import { getMciList } from '../api/tumblebug';
import { CSP_METRICS } from '../api/csp';
import MetricChart from '../components/MetricChart';
import ProviderBadge from '../components/ProviderBadge';
import useBasePath from '../hooks/useBasePath';

export default function K8sNodeDashboard() {
  const { nsId, connectionName: routeConn, clusterName: routeCluster, nodeGroupName: routeNg, nodeNumber: routeNode } = useParams();
  const navigate = useNavigate();
  const base = useBasePath();

  // Cluster discovery
  const [allClusters, setAllClusters] = useState([]); // { cluster, connectionName }
  const [selectedConn, setSelectedConn] = useState(routeConn || '');
  const [selectedCluster, setSelectedCluster] = useState(routeCluster || '');
  const [clusterDetail, setClusterDetail] = useState(null);
  const [selectedNg, setSelectedNg] = useState(routeNg || '');
  const [selectedNode, setSelectedNode] = useState(routeNode || '');
  const [clustersLoading, setClustersLoading] = useState(true);

  // Metrics
  const [selectedMetric, setSelectedMetric] = useState('cpu_usage');
  const [selectedRange, setSelectedRange] = useState('1');
  const [allMetrics, setAllMetrics] = useState(null);
  const [loading, setLoading] = useState(false);

  // Load clusters from all connections in NS
  useEffect(() => {
    if (!nsId) return;
    setClustersLoading(true);
    (async () => {
      let mcis = [];
      try { mcis = await getMciList(nsId); } catch {}
      const connNames = [...new Set(mcis.flatMap(m => (m.vm || []).map(v => v.connectionName)).filter(Boolean))];
      const results = await Promise.allSettled(connNames.map(async (conn) => {
        const list = await getClusters(conn);
        return list.map(c => ({ name: c.IId?.NameId, connectionName: conn }));
      }));
      setAllClusters(results.filter(r => r.status === 'fulfilled').flatMap(r => r.value));
    })().finally(() => setClustersLoading(false));
  }, [nsId]);

  // Load cluster detail when cluster selected
  useEffect(() => {
    if (!selectedConn || !selectedCluster) { setClusterDetail(null); return; }
    getCluster(selectedConn, selectedCluster).then(setClusterDetail).catch(() => setClusterDetail(null));
  }, [selectedConn, selectedCluster]);

  // Nodes from detail
  const nodeGroups = clusterDetail?.NodeGroupList || [];
  const selectedNgObj = nodeGroups.find(ng => ng.IId?.NameId === selectedNg);
  const nodes = selectedNgObj?.Nodes || [];

  // Load metrics when node selected
  useEffect(() => {
    if (!selectedConn || !selectedCluster || !selectedNg || !selectedNode) { setAllMetrics(null); return; }
    setLoading(true);
    setAllMetrics(null);
    getAllClusterNodeMetrics(selectedConn, selectedCluster, selectedNg, selectedNode, selectedRange)
      .then(setAllMetrics)
      .catch(() => setAllMetrics({}))
      .finally(() => setLoading(false));
  }, [selectedConn, selectedCluster, selectedNg, selectedNode, selectedRange]);

  // Update URL when selection changes (without full reload)
  function updateSelection(conn, cluster, ng, node) {
    if (conn && cluster && ng && node) {
      navigate(`${base}/monitoring/${nsId}/k8s/${conn}/${cluster}/${ng}/${node}`, { replace: true });
    }
  }

  const activeData = allMetrics?.[selectedMetric];
  const overviewKeys = ['cpu_usage', 'memory_usage', 'network_in'];

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-sm">K8s Node Monitoring</span>
            {selectedConn && <ProviderBadge connectionName={selectedConn} />}
          </div>
        </div>
        <div className="p-4 space-y-4">
          {/* Cascade selectors */}
          <div className="grid grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Cluster</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={`${selectedConn}|${selectedCluster}`}
                onChange={(e) => {
                  const [c, cl] = e.target.value.split('|');
                  setSelectedConn(c); setSelectedCluster(cl); setSelectedNg(''); setSelectedNode('');
                }}>
                <option value="|">{clustersLoading ? 'Loading...' : 'Select'}</option>
                {allClusters.map(c => (
                  <option key={`${c.connectionName}|${c.name}`} value={`${c.connectionName}|${c.name}`}>{c.name}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Node Group</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedNg}
                onChange={(e) => { setSelectedNg(e.target.value); setSelectedNode(''); }} disabled={!clusterDetail}>
                <option value="">Select</option>
                {nodeGroups.map(ng => (
                  <option key={ng.IId?.NameId} value={ng.IId?.NameId}>{ng.IId?.NameId}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Node</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedNode}
                onChange={(e) => { setSelectedNode(e.target.value); updateSelection(selectedConn, selectedCluster, selectedNg, e.target.value); }} disabled={!selectedNg}>
                <option value="">Select</option>
                {nodes.map((n, i) => (
                  <option key={i + 1} value={String(i + 1)}>Node {i + 1} — {n.NameId || n.SystemId || `#${i+1}`}</option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Range</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedRange} onChange={(e) => setSelectedRange(e.target.value)}>
                <option value="1">1H</option>
                <option value="6">6H</option>
                <option value="12">12H</option>
                <option value="24">1D</option>
                <option value="72">3D</option>
                <option value="168">7D</option>
              </select>
            </div>
          </div>

          {/* Metric selector */}
          {selectedNode && (
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">API Metric</label>
              <div className="flex gap-1 flex-wrap">
                {CSP_METRICS.map((m) => (
                  <button key={m.key} onClick={() => setSelectedMetric(m.key)}
                    className={`px-3 py-1.5 text-xs rounded-md border ${selectedMetric === m.key ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'}`}>
                    {m.label}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* Charts — only when node is selected */}
        {selectedNode && (
          <>
            {/* Selected metric chart */}
            <div className="p-4 border-t">
              <div className="mb-2 text-sm font-medium">{activeData?.metricName || selectedMetric}</div>
              {(loading || !allMetrics) ? (
                <div className="flex items-center justify-center h-[300px] text-gray-400 animate-pulse">Loading CSP API data...</div>
              ) : activeData?.series ? (
                <div className="bg-white rounded border p-3">
                  <MetricChart title={`${activeData.metricName} (${activeData.metricUnit})`} series={activeData.series} height={300} />
                </div>
              ) : (
                <div className="flex items-center justify-center h-[300px] text-gray-400">No data</div>
              )}
            </div>

            {/* Overview */}
            <div className="p-4 border-t">
              <div className="text-xs text-gray-500 mb-2">Node Overview</div>
              {(loading || !allMetrics) ? (
                <div className="flex items-center justify-center h-[160px] text-gray-400 animate-pulse">Loading CSP API data...</div>
              ) : (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
                  {overviewKeys.map(key => {
                    const d = allMetrics[key];
                    return (
                      <div key={key} className="bg-white rounded border p-3 cursor-pointer hover:ring-1 hover:ring-blue-300"
                        onClick={() => setSelectedMetric(key)}>
                        <MetricChart title={d?.metricName || key} series={d?.series || []} height={140} />
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </>
        )}

        {!selectedNode && (
          <div className="p-8 text-center text-sm text-gray-400">Select a cluster, node group, and node to view metrics</div>
        )}
      </div>
    </div>
  );
}
