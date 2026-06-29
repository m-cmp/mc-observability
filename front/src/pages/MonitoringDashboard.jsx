import { useState, useEffect, useCallback } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { getMeasurementFields, getMetricsByNode } from '../api/monitoring';
import { getInfra } from '../api/tumblebug';
import { getPlugins, getPrediction, getDetectionHistory } from '../api/monitoring';
import { getAllCspMetrics, CSP_METRICS, isCspSupported } from '../api/csp';
import { getNodeItems, getNodeList } from '../api/node';
import MetricChart from '../components/MetricChart';
import AgentNotInstalled from '../components/AgentNotInstalled';

const RANGE_OPTIONS = [
  { value: '1h', label: '1H' },
  { value: '6h', label: '6H' },
  { value: '12h', label: '12H' },
  { value: '1d', label: '1D' },
  { value: '3d', label: '3D' },
  { value: '5d', label: '5D' },
  { value: '7d', label: '7D' },
];

const PERIOD_OPTIONS = [
  { value: '1m', label: '1m' },
  { value: '3m', label: '3m' },
  { value: '5m', label: '5m' },
  { value: '10m', label: '10m' },
  { value: '15m', label: '15m' },
  { value: '30m', label: '30m' },
  { value: '1h', label: '1h' },
];

const AGG_OPTIONS = [
  { value: 'mean', label: 'Mean' },
  { value: 'max', label: 'Max' },
  { value: 'min', label: 'Min' },
  { value: 'last', label: 'Last' },
  { value: 'sum', label: 'Sum' },
];

export default function MonitoringDashboard() {
  const { nsId, infraId, nodeId: routeNodeId } = useParams();
  const [searchParams] = useSearchParams();
  const initialSource = searchParams.get('source') || 'agent';
  // K8s agent node detail: infraId is the clusterId and routeNodeId is the k8s node name.
  // Metrics live in InfluxDB under the same (ns_id, infra_id, node_id) schema, but the node
  // is NOT a Tumblebug VM — so skip the VM infra/node-list lookups.
  const isK8s = searchParams.get('k8s') === '1';

  // Cascade selectors
  const [nodes, setNodes] = useState([]);
  const [selectedNodeId, setSelectedNodeId] = useState(routeNodeId || '');
  const [measurements, setMeasurements] = useState([]);
  const [selectedMeasurement, setSelectedMeasurement] = useState('');
  const [metrics, setMetrics] = useState([]);
  const [selectedMetric, setSelectedMetric] = useState('');
  const [selectedAgg, setSelectedAgg] = useState('mean');
  const [selectedRange, setSelectedRange] = useState('1h');
  const [selectedPeriod, setSelectedPeriod] = useState('1m');

  // Toggles
  const [predictionEnabled, setPredictionEnabled] = useState(false);
  const [detectionEnabled, setDetectionEnabled] = useState(false);

  // Auto-refresh
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState(30);

  // Data source: agent (InfluxDB) or csp (cb-spider)
  const [dataSource, setDataSource] = useState(initialSource);
  const [cspMetrics, setCspMetrics] = useState(null);
  const [selectedCspMetric, setSelectedCspMetric] = useState('cpu_usage');
  const [cspNodeInfo, setCspNodeInfo] = useState(null); // { connectionName, cspResourceName }

  // Chart data
  const [chartSeries, setChartSeries] = useState([]);
  const [chartTitle, setChartTitle] = useState('');
  const [chartMeasurement, setChartMeasurement] = useState('');
  const [chartMetric, setChartMetric] = useState('');
  const [detectionSeries, setDetectionSeries] = useState([]);
  const [loading, setLoading] = useState(false);

  // Auto-loaded overview charts (always shown when Node is selected). null = not loaded yet
  const [overviewCharts, setOverviewCharts] = useState(null);

  // All measurement fields (for metric dropdown)
  const [allFields, setAllFields] = useState([]);

  // Load Nodes from Tumblebug
  const [nodesLoaded, setNodesLoaded] = useState(false);
  useEffect(() => {
    if (!nsId || !infraId) return;
    setNodesLoaded(false);
    // K8s agent node: not a Tumblebug VM infra — synthesize the single node from the route.
    if (isK8s) {
      setNodes(routeNodeId ? [{ id: routeNodeId, name: routeNodeId }] : []);
      setNodesLoaded(true);
      return;
    }
    getInfra(nsId, infraId)
      .then((data) => { setNodes(data.node || []); setNodesLoaded(true); })
      .catch(() => { setNodes([]); setNodesLoaded(true); });
  }, [nsId, infraId, isK8s, routeNodeId]);

  // Load all measurement fields (for metric dropdown options)
  const [activeItemNames, setActiveItemNames] = useState(null); // null = not loaded, Set = loaded
  useEffect(() => {
    getMeasurementFields().then(setAllFields).catch(() => setAllFields([]));
  }, []);

  // Detect whether the monitoring agent is installed on the selected Node.
  // null = unknown (don't show the notice), true/false otherwise.
  const [agentInstalled, setAgentInstalled] = useState(null);
  useEffect(() => {
    if (!nsId || !infraId || !selectedNodeId) { setAgentInstalled(null); return; }
    // K8s agent nodes are not in the VM node list; we arrived here because the agent
    // is installed and reporting metrics, so don't show the "not installed" notice.
    if (isK8s) { setAgentInstalled(true); return; }
    let alive = true;
    getNodeList(nsId, infraId)
      .then((list) => {
        if (!alive) return;
        const found = (list || []).find((n) => (n.node_id || n.id) === selectedNodeId);
        // A node only appears in the o11y list once its agent has been installed.
        setAgentInstalled(!!found);
      })
      .catch(() => { if (alive) setAgentInstalled(null); });
    return () => { alive = false; };
  }, [nsId, infraId, selectedNodeId, isK8s]);

  // Load active items for selected Node → filter measurements
  useEffect(() => {
    if (!nsId || !infraId || !selectedNodeId) { setActiveItemNames(null); setMeasurements([]); return; }
    getNodeItems(nsId, infraId, selectedNodeId)
      .then((items) => {
        const names = new Set(items.map((it) => it.pluginName || it.name));
        setActiveItemNames(names);
      })
      .catch(() => setActiveItemNames(null));
  }, [nsId, infraId, selectedNodeId]);

  // Filter measurements to only those active on the Node
  useEffect(() => {
    if (!activeItemNames) {
      // Fallback: show all plugins if items not loaded
      getPlugins()
        .then((data) => setMeasurements(data.filter((p) => p.pluginType === 'INPUT').map((p) => p.name || p.pluginId)))
        .catch(() => setMeasurements([]));
      return;
    }
    setMeasurements([...activeItemNames]);
  }, [activeItemNames]);

  // When measurement changes, populate metrics
  useEffect(() => {
    if (!selectedMeasurement) {
      setMetrics([]);
      setSelectedMetric('');
      return;
    }
    const found = allFields.find((f) => f.measurement === selectedMeasurement);
    let fieldKeys = found && found.fields ? found.fields.map((f) => f.key) : [];
    // Add virtual "usage_percent" for cpu (100 - usage_idle)
    if (selectedMeasurement === 'cpu' && fieldKeys.includes('usage_idle') && !fieldKeys.includes('usage_percent')) {
      fieldKeys = ['usage_percent', ...fieldKeys];
    }
    setMetrics(fieldKeys);
    setSelectedMetric('');
  }, [selectedMeasurement, allFields]);

  // Set route nodeId
  useEffect(() => {
    if (routeNodeId) setSelectedNodeId(routeNodeId);
  }, [routeNodeId]);

  // Track CSP info for selected Node. K8s nodes are NOT cb-spider VMs — their CSP metrics live
  // behind the cluster-node endpoint (see K8sNodeDashboard / the Infra overview K8s tab), so don't
  // offer the VM-based API source here for a k8s node (it would 404/empty on cb-spider).
  useEffect(() => {
    const node = nodes.find((n) => n.id === selectedNodeId);
    if (!isK8s && node && node.connectionName && node.cspResourceName) {
      setCspNodeInfo({ connectionName: node.connectionName, cspResourceName: node.cspResourceName });
    } else {
      setCspNodeInfo(null);
    }
  }, [selectedNodeId, nodes, isK8s]);

  // Load CSP metrics when dataSource=csp (wait for nodes to load first)
  const [cspLoading, setCspLoading] = useState(false);
  useEffect(() => {
    if (!nodesLoaded || dataSource !== 'csp' || !cspNodeInfo) { setCspMetrics(null); return; }
    setCspLoading(true);
    setCspMetrics(null);
    const hours = selectedRange.endsWith('d') ? parseInt(selectedRange) * 24 : parseInt(selectedRange);
    getAllCspMetrics(cspNodeInfo.connectionName, cspNodeInfo.cspResourceName, String(hours || 1))
      .then(setCspMetrics)
      .catch(() => setCspMetrics({}))
      .finally(() => setCspLoading(false));
  }, [dataSource, cspNodeInfo, selectedRange, nodesLoaded]);

  // Overview loader
  const [overviewLoading, setOverviewLoading] = useState(false);
  const loadOverview = useCallback(async () => {
    if (!nsId || !infraId || !selectedNodeId) { setOverviewCharts(null); return; }
    setOverviewLoading(true);
    const overviewMetrics = [
      { measurement: 'cpu', field: 'usage_idle', title: 'CPU Used', unit: '%', invert: true },
      { measurement: 'mem', field: 'used_percent', title: 'Memory Used', unit: '%' },
      { measurement: 'disk', field: 'used_percent', title: 'Disk Used', unit: '%' },
    ];
    const results = await Promise.allSettled(
      overviewMetrics.map(async (m) => {
        const data = await getMetricsByNode(nsId, infraId, selectedNodeId, {
          measurement: m.measurement, range: selectedRange, groupTime: selectedPeriod,
          fields: [{ function: 'mean', field: m.field }],
        });
        const series = toChartSeries(data, m.field);
        if (m.invert) {
          series.forEach((s) => { s.name = m.title; s.data = s.data.map((d) => ({ ...d, y: 100 - d.y })); });
        }
        return { ...m, series };
      })
    );
    setOverviewCharts(results.filter((r) => r.status === 'fulfilled').map((r) => r.value));
    setOverviewLoading(false);
  }, [nsId, infraId, selectedNodeId, selectedRange, selectedPeriod]);

  // Load on mount + when params change
  useEffect(() => { loadOverview(); }, [loadOverview]);

  // Auto-refresh timer
  useEffect(() => {
    if (!autoRefresh || !selectedNodeId) return;
    const id = setInterval(() => { loadOverview(); }, refreshInterval * 1000);
    return () => clearInterval(id);
  }, [autoRefresh, refreshInterval, loadOverview, selectedNodeId]);

  const startMonitoring = useCallback(async () => {
    if (!selectedNodeId || !selectedMeasurement || !selectedMetric) {
      alert('Please select Node, Measurement, and Metric.');
      return;
    }
    setLoading(true);
    try {
      const isVirtualPercent = selectedMeasurement === 'cpu' && selectedMetric === 'usage_percent';
      const actualField = isVirtualPercent ? 'usage_idle' : selectedMetric;
      const displayMetric = isVirtualPercent ? 'usage_percent' : selectedMetric;

      const data = await getMetricsByNode(nsId, infraId, selectedNodeId, {
        measurement: selectedMeasurement,
        range: selectedRange,
        groupTime: selectedPeriod,
        fields: [{ function: selectedAgg, field: actualField }],
      });
      let series = toChartSeries(data, displayMetric);
      if (isVirtualPercent) {
        series = series.map((s) => ({
          ...s,
          name: 'usage_percent',
          data: s.data.map((d) => ({ ...d, y: 100 - d.y })),
        }));
      }
      setChartSeries(series);
      setChartTitle(`${selectedMeasurement.toUpperCase()} - ${displayMetric}`);
      setChartMeasurement(selectedMeasurement);
      setChartMetric(displayMetric);

      // Prediction overlay
      if (predictionEnabled) {
        try {
          const pred = await getPrediction(nsId, infraId, selectedNodeId, selectedMeasurement);
          if (pred && pred.values && pred.values.length > 0) {
            const predSeries = {
              name: `${selectedMetric} (Predicted)`,
              data: pred.values.map((v) => ({ x: new Date(v.timestamp).getTime(), y: parseFloat(v.value) })),
            };
            setChartSeries((prev) => [...prev, predSeries]);
          }
        } catch {}
      }

      // Detection chart
      if (detectionEnabled) {
        try {
          const det = await getDetectionHistory(nsId, infraId, selectedNodeId, selectedMeasurement);
          if (det && det.values && det.values.length > 0) {
            setDetectionSeries([{
              name: 'Anomaly Score',
              data: det.values.map((v) => ({ x: new Date(v.timestamp).getTime(), y: v.anomaly_score })),
            }]);
          } else {
            setDetectionSeries([]);
          }
        } catch {
          setDetectionSeries([]);
        }
      }
    } catch (e) {
      console.error('startMonitoring failed', e);
    }
    setLoading(false);
  }, [nsId, infraId, selectedNodeId, selectedMeasurement, selectedMetric, selectedAgg, selectedRange, selectedPeriod, predictionEnabled, detectionEnabled]);

  return (
    <div className="space-y-4">
      {/* Card: Monitoring Trend / Workload */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex items-center justify-between">
          <span className="font-semibold text-sm">Monitoring Trend / Workload</span>
          {cspNodeInfo && (
            <div className="flex bg-gray-100 rounded-lg p-0.5 text-xs">
              <button onClick={() => setDataSource('agent')}
                className={`px-3 py-1 rounded-md ${dataSource === 'agent' ? 'bg-white shadow text-blue-600 font-medium' : 'text-gray-500'}`}>Agent</button>
              <button onClick={() => setDataSource('csp')} title="CSP API Based"
                className={`px-3 py-1 rounded-md ${dataSource === 'csp' ? 'bg-white shadow text-blue-600 font-medium' : 'text-gray-500'}`}>API</button>
            </div>
          )}
        </div>
        <div className="p-4 space-y-4">
          {/* Row 1: Workload (readonly) + Server */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Workload</label>
              <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={infraId || ''} readOnly />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Server</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedNodeId} onChange={(e) => setSelectedNodeId(e.target.value)}>
                <option value="">Select</option>
                {nodes.map((node) => {
                  const id = node.id || node.node_id || node.name;
                  return <option key={id} value={id}>{node.name || id}</option>;
                })}
              </select>
            </div>
          </div>

          {/* Agent-specific controls */}
          {dataSource === 'agent' && <>
          {/* Row 2: Measurement, Metric, Aggregation */}
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Measurement</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedMeasurement} onChange={(e) => setSelectedMeasurement(e.target.value)}>
                <option value="">Select</option>
                {measurements.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Metric</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedMetric} onChange={(e) => setSelectedMetric(e.target.value)}>
                <option value="">Select</option>
                {metrics.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Aggregation</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedAgg} onChange={(e) => setSelectedAgg(e.target.value)}>
                {AGG_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
          </div>

          {/* Row 3: Range, Period, Prediction */}
          <div className="grid grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Range</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedRange} onChange={(e) => setSelectedRange(e.target.value)}>
                {RANGE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Period</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedPeriod} onChange={(e) => setSelectedPeriod(e.target.value)}>
                {PERIOD_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
            <div />
          </div>

          {/* Switches */}
          <div className="flex gap-6 flex-wrap items-center">
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" className="rounded" checked={predictionEnabled} onChange={(e) => setPredictionEnabled(e.target.checked)} />
              Extend Prediction
            </label>
            <label className="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" className="rounded" checked={detectionEnabled} onChange={(e) => setDetectionEnabled(e.target.checked)} />
              Extend Detection
            </label>
            <div className="ml-auto flex items-center gap-2">
              <label className="flex items-center gap-2 text-sm cursor-pointer">
                <input type="checkbox" className="rounded" checked={autoRefresh} onChange={(e) => setAutoRefresh(e.target.checked)} />
                Auto Refresh
              </label>
              {autoRefresh && (
                <select className="border border-gray-300 rounded px-2 py-1 text-xs" value={refreshInterval} onChange={(e) => setRefreshInterval(+e.target.value)}>
                  <option value={10}>10s</option>
                  <option value={30}>30s</option>
                  <option value={60}>1m</option>
                </select>
              )}
              {autoRefresh && <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" title="Auto-refreshing" />}
            </div>
          </div>

          {/* Start Monitoring button */}
          <div className="text-center">
            <button
              onClick={startMonitoring}
              disabled={loading}
              className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm font-medium"
            >
              {loading ? 'Loading...' : 'Start Monitoring'}
            </button>
          </div>
          </>}

          {/* CSP metric selector */}
          {dataSource === 'csp' && selectedNodeId && (
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">API Metric</label>
              <div className="flex gap-1 flex-wrap">
                {CSP_METRICS.map((m) => (
                  <button key={m.key} onClick={() => setSelectedCspMetric(m.key)}
                    className={`px-3 py-1.5 text-xs rounded-md border ${selectedCspMetric === m.key ? 'bg-blue-600 text-white border-blue-600' : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'}`}>
                    {m.label}
                  </button>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* CSP chart */}
        {dataSource === 'csp' && selectedNodeId && (
          <div className="p-4 border-t">
            <div className="mb-2 text-sm font-medium">
              API: {nodes.find(n => n.id === selectedNodeId)?.name || selectedNodeId}
            </div>
            {(cspLoading || !cspMetrics) ? (
              <div className="flex items-center justify-center h-[300px] text-gray-400 animate-pulse">Loading CSP API data...</div>
            ) : cspMetrics[selectedCspMetric]?.series ? (
              <div className="bg-white rounded border p-3">
                <MetricChart
                  title={`${cspMetrics[selectedCspMetric].metricName} (${cspMetrics[selectedCspMetric].metricUnit})`}
                  series={cspMetrics[selectedCspMetric].series}
                  height={300}
                />
              </div>
            ) : (
              <div className="flex items-center justify-center h-[300px] text-gray-400">No data</div>
            )}
          </div>
        )}

        {/* Overview charts — always shown when Node selected (agent mode only) */}
        {dataSource === 'agent' && selectedNodeId && (
          <div className="p-4 border-t">
            <div className="mb-2 text-sm font-medium">
              Node: {nodes.find(n => n.id === selectedNodeId)?.name || selectedNodeId}
            </div>
            {agentInstalled === false ? (
              <AgentNotInstalled nsId={nsId} infraId={infraId} nodeId={selectedNodeId} height={160}
                nodeStatus={nodes.find(n => n.id === selectedNodeId)?.status} />
            ) : (overviewLoading || !overviewCharts) ? (
              <div className="flex items-center justify-center h-[160px] text-gray-400 animate-pulse">Loading metrics...</div>
            ) : overviewCharts.length > 0 ? (
              <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
                {overviewCharts.map((oc) => (
                  <div key={oc.measurement} className="bg-white rounded border p-3">
                    <MetricChart title={oc.title} series={oc.series} height={160} measurement={oc.measurement} metric={oc.field} />
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex items-center justify-center h-[160px] text-gray-400">No data</div>
            )}
          </div>
        )}

        {/* Custom query chart (agent only) */}
        {dataSource === 'agent' && chartSeries.length > 0 && (
          <div className="p-4 border-t">
            <div className="text-xs text-gray-500 mb-2">Custom Query — Trend Graph</div>
            <div className="bg-white rounded border p-3">
              <MetricChart title={chartTitle} series={chartSeries} height={240} measurement={chartMeasurement} metric={chartMetric} />
            </div>

            {detectionEnabled && detectionSeries.length > 0 && (
              <div className="mt-4">
                <div className="text-xs text-gray-500 mb-2">Detection Graph</div>
                <div className="bg-white rounded border p-3">
                  <MetricChart title="Anomaly Score Over Time" series={detectionSeries} height={240} chartType="line" />
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}

function toChartSeries(metricDTOs, metricName) {
  if (!metricDTOs || metricDTOs.length === 0) return [];
  return metricDTOs.map((m) => {
    const timeIdx = (m.columns || []).indexOf('timestamp');
    const valIdx = timeIdx === 0 ? 1 : 0;
    return {
      name: metricName || m.columns?.[valIdx] || m.name || 'value',
      data: (m.values || [])
        .filter((row) => row[valIdx] !== null && row[valIdx] !== undefined)
        .map((row) => ({
          x: typeof row[timeIdx] === 'string' ? new Date(row[timeIdx]).getTime() : row[timeIdx],
          y: parseFloat(row[valIdx]),
        })),
    };
  });
}
