import { useState, useEffect, useCallback } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { getMeasurementFields, getMetricsByVM } from '../api/monitoring';
import { getMci } from '../api/tumblebug';
import { getPlugins, getPrediction, getDetectionHistory } from '../api/monitoring';
import { getAllCspMetrics, CSP_METRICS, isCspSupported } from '../api/csp';
import { getVmItems } from '../api/vm';
import MetricChart from '../components/MetricChart';

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
  const { nsId, mciId, vmId: routeVmId } = useParams();
  const [searchParams] = useSearchParams();
  const initialSource = searchParams.get('source') || 'agent';

  // Cascade selectors
  const [vms, setVms] = useState([]);
  const [selectedVmId, setSelectedVmId] = useState(routeVmId || '');
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
  const [cspVmInfo, setCspVmInfo] = useState(null); // { connectionName, cspResourceName }

  // Chart data
  const [chartSeries, setChartSeries] = useState([]);
  const [chartTitle, setChartTitle] = useState('');
  const [chartMeasurement, setChartMeasurement] = useState('');
  const [chartMetric, setChartMetric] = useState('');
  const [detectionSeries, setDetectionSeries] = useState([]);
  const [loading, setLoading] = useState(false);

  // Auto-loaded overview charts (always shown when VM is selected). null = not loaded yet
  const [overviewCharts, setOverviewCharts] = useState(null);

  // All measurement fields (for metric dropdown)
  const [allFields, setAllFields] = useState([]);

  // Load VMs from Tumblebug
  const [vmsLoaded, setVmsLoaded] = useState(false);
  useEffect(() => {
    if (!nsId || !mciId) return;
    setVmsLoaded(false);
    getMci(nsId, mciId)
      .then((data) => { setVms(data.vm || []); setVmsLoaded(true); })
      .catch(() => { setVms([]); setVmsLoaded(true); });
  }, [nsId, mciId]);

  // Load all measurement fields (for metric dropdown options)
  const [activeItemNames, setActiveItemNames] = useState(null); // null = not loaded, Set = loaded
  useEffect(() => {
    getMeasurementFields().then(setAllFields).catch(() => setAllFields([]));
  }, []);

  // Load active items for selected VM → filter measurements
  useEffect(() => {
    if (!nsId || !mciId || !selectedVmId) { setActiveItemNames(null); setMeasurements([]); return; }
    getVmItems(nsId, mciId, selectedVmId)
      .then((items) => {
        const names = new Set(items.map((it) => it.pluginName || it.name));
        setActiveItemNames(names);
      })
      .catch(() => setActiveItemNames(null));
  }, [nsId, mciId, selectedVmId]);

  // Filter measurements to only those active on the VM
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

  // Set route vmId
  useEffect(() => {
    if (routeVmId) setSelectedVmId(routeVmId);
  }, [routeVmId]);

  // Track CSP info for selected VM
  useEffect(() => {
    const vm = vms.find((v) => v.id === selectedVmId);
    if (vm && vm.connectionName && vm.cspResourceName) {
      setCspVmInfo({ connectionName: vm.connectionName, cspResourceName: vm.cspResourceName });
    } else {
      setCspVmInfo(null);
    }
  }, [selectedVmId, vms]);

  // Load CSP metrics when dataSource=csp (wait for vms to load first)
  const [cspLoading, setCspLoading] = useState(false);
  useEffect(() => {
    if (!vmsLoaded || dataSource !== 'csp' || !cspVmInfo) { setCspMetrics(null); return; }
    setCspLoading(true);
    setCspMetrics(null);
    const hours = selectedRange.endsWith('d') ? parseInt(selectedRange) * 24 : parseInt(selectedRange);
    getAllCspMetrics(cspVmInfo.connectionName, cspVmInfo.cspResourceName, String(hours || 1))
      .then(setCspMetrics)
      .catch(() => setCspMetrics({}))
      .finally(() => setCspLoading(false));
  }, [dataSource, cspVmInfo, selectedRange, vmsLoaded]);

  // Overview loader
  const [overviewLoading, setOverviewLoading] = useState(false);
  const loadOverview = useCallback(async () => {
    if (!nsId || !mciId || !selectedVmId) { setOverviewCharts(null); return; }
    setOverviewLoading(true);
    const overviewMetrics = [
      { measurement: 'cpu', field: 'usage_idle', title: 'CPU Used', unit: '%', invert: true },
      { measurement: 'mem', field: 'used_percent', title: 'Memory Used', unit: '%' },
      { measurement: 'disk', field: 'used_percent', title: 'Disk Used', unit: '%' },
    ];
    const results = await Promise.allSettled(
      overviewMetrics.map(async (m) => {
        const data = await getMetricsByVM(nsId, mciId, selectedVmId, {
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
  }, [nsId, mciId, selectedVmId, selectedRange, selectedPeriod]);

  // Load on mount + when params change
  useEffect(() => { loadOverview(); }, [loadOverview]);

  // Auto-refresh timer
  useEffect(() => {
    if (!autoRefresh || !selectedVmId) return;
    const id = setInterval(() => { loadOverview(); }, refreshInterval * 1000);
    return () => clearInterval(id);
  }, [autoRefresh, refreshInterval, loadOverview, selectedVmId]);

  const startMonitoring = useCallback(async () => {
    if (!selectedVmId || !selectedMeasurement || !selectedMetric) {
      alert('Please select VM, Measurement, and Metric.');
      return;
    }
    setLoading(true);
    try {
      const isVirtualPercent = selectedMeasurement === 'cpu' && selectedMetric === 'usage_percent';
      const actualField = isVirtualPercent ? 'usage_idle' : selectedMetric;
      const displayMetric = isVirtualPercent ? 'usage_percent' : selectedMetric;

      const data = await getMetricsByVM(nsId, mciId, selectedVmId, {
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
          const pred = await getPrediction(nsId, mciId, selectedVmId, selectedMeasurement);
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
          const det = await getDetectionHistory(nsId, mciId, selectedVmId, selectedMeasurement);
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
  }, [nsId, mciId, selectedVmId, selectedMeasurement, selectedMetric, selectedAgg, selectedRange, selectedPeriod, predictionEnabled, detectionEnabled]);

  return (
    <div className="space-y-4">
      {/* Card: Monitoring Trend / Workload */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex items-center justify-between">
          <span className="font-semibold text-sm">Monitoring Trend / Workload</span>
          {cspVmInfo && (
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
              <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={mciId || ''} readOnly />
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Server</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedVmId} onChange={(e) => setSelectedVmId(e.target.value)}>
                <option value="">Select</option>
                {vms.map((vm) => {
                  const id = vm.id || vm.vm_id || vm.name;
                  return <option key={id} value={id}>{vm.name || id}</option>;
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
          {dataSource === 'csp' && selectedVmId && (
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
        {dataSource === 'csp' && selectedVmId && (
          <div className="p-4 border-t">
            <div className="mb-2 text-sm font-medium">
              API: {vms.find(v => v.id === selectedVmId)?.name || selectedVmId}
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

        {/* Overview charts — always shown when VM selected (agent mode only) */}
        {dataSource === 'agent' && selectedVmId && (
          <div className="p-4 border-t">
            <div className="mb-2 text-sm font-medium">
              VM: {vms.find(v => v.id === selectedVmId)?.name || selectedVmId}
            </div>
            {(overviewLoading || !overviewCharts) ? (
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
