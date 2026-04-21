import { useState, useEffect, useCallback } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { getClusters, getCluster, getClusterNodeMetric } from '../api/k8s';
import { CSP_METRICS, isCspSupported } from '../api/csp';
import MetricChart from '../components/MetricChart';
import ProviderBadge from '../components/ProviderBadge';

export default function K8sNodeDashboard() {
  const { nsId, connectionName, clusterName, nodeGroupName, nodeNumber } = useParams();
  const [searchParams] = useSearchParams();

  const [selectedMetric, setSelectedMetric] = useState('cpu_usage');
  const [selectedRange, setSelectedRange] = useState('1');
  const [chartData, setChartData] = useState(null);
  const [overviewData, setOverviewData] = useState({});
  const [loading, setLoading] = useState(false);
  const [overviewLoading, setOverviewLoading] = useState(true);

  // Auto-load overview metrics
  useEffect(() => {
    if (!connectionName || !clusterName || !nodeGroupName || !nodeNumber) return;
    setOverviewLoading(true);
    const OVERVIEW = ['cpu_usage', 'memory_usage', 'network_in'];
    Promise.allSettled(OVERVIEW.map(async (mt) => {
      const data = await getClusterNodeMetric(connectionName, clusterName, nodeGroupName, nodeNumber, mt, '5', selectedRange);
      return { key: mt, data };
    })).then(results => {
      const d = {};
      results.forEach(r => { if (r.status === 'fulfilled') d[r.value.key] = r.value.data; });
      setOverviewData(d);
    }).finally(() => setOverviewLoading(false));
  }, [connectionName, clusterName, nodeGroupName, nodeNumber, selectedRange]);

  const loadMetric = useCallback(async () => {
    if (!connectionName || !clusterName || !nodeGroupName || !nodeNumber || !selectedMetric) return;
    setLoading(true);
    try {
      const data = await getClusterNodeMetric(connectionName, clusterName, nodeGroupName, nodeNumber, selectedMetric, '5', selectedRange);
      setChartData(data);
    } catch { setChartData(null); }
    setLoading(false);
  }, [connectionName, clusterName, nodeGroupName, nodeNumber, selectedMetric, selectedRange]);

  function toSeries(data) {
    if (!data?.timestampValues?.length) return [];
    return [{ name: data.metricName || 'value', data: data.timestampValues.map(v => ({ x: new Date(v.timestamp).getTime(), y: parseFloat(v.value) })) }];
  }

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="font-semibold text-sm">K8s Node Monitoring</span>
            <ProviderBadge connectionName={connectionName} />
          </div>
        </div>
        <div className="p-4 space-y-4">
          {/* Info row */}
          <div className="grid grid-cols-4 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Cluster</label>
              <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={clusterName || ''} readOnly />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Node Group</label>
              <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={nodeGroupName || ''} readOnly />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Node #</label>
              <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={nodeNumber || ''} readOnly />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Range (hours)</label>
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
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">API Metric</label>
            <div className="flex gap-1 flex-wrap">
              {CSP_METRICS.map((m) => (
                <button key={m.key} onClick={() => { setSelectedMetric(m.key); }}
                  className={`px-3 py-1.5 text-xs rounded-md border ${selectedMetric === m.key ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'}`}>
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          <div className="text-center">
            <button onClick={loadMetric} disabled={loading} className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm font-medium">
              {loading ? 'Loading...' : 'Load Metric'}
            </button>
          </div>
        </div>

        {/* Overview charts */}
        <div className="p-4 border-t">
          <div className="text-xs text-gray-500 mb-2">Node Overview</div>
          {overviewLoading ? (
            <div className="flex items-center justify-center h-[160px] text-gray-400 animate-pulse">Loading CSP API data...</div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
              {['cpu_usage', 'memory_usage', 'network_in'].map(key => {
                const d = overviewData[key];
                const label = CSP_METRICS.find(m => m.key === key)?.label || key;
                return (
                  <div key={key} className="bg-white rounded border p-3">
                    <MetricChart title={d?.metricName || label} series={toSeries(d)} height={160} />
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {/* Custom metric chart */}
        {chartData && (
          <div className="p-4 border-t">
            <div className="text-xs text-gray-500 mb-2">Custom Query</div>
            <div className="bg-white rounded border p-3">
              <MetricChart title={`${chartData.metricName || selectedMetric} (${chartData.metricUnit || ''})`} series={toSeries(chartData)} height={300} />
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
