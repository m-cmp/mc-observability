import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMci } from '../api/tumblebug';
import { getMetricsByVM } from '../api/monitoring';
import { getAllCspMetrics, isCspSupported } from '../api/csp';
import MetricChart from '../components/MetricChart';

const AGENT_METRICS = [
  { key: 'cpu', measurement: 'cpu', field: 'usage_idle', label: 'CPU Used', unit: '%', invert: true },
  { key: 'mem', measurement: 'mem', field: 'used_percent', label: 'Memory Used', unit: '%' },
  { key: 'disk', measurement: 'disk', field: 'used_percent', label: 'Disk Used', unit: '%' },
];

const CSP_OVERVIEW_KEYS = ['cpu_usage', 'memory_usage', 'network_in'];

export default function MciOverview() {
  const { nsId, mciId } = useParams();
  const navigate = useNavigate();
  const [vms, setVms] = useState([]);
  const [vmData, setVmData] = useState({});
  const [loading, setLoading] = useState(true);
  const [metricsLoading, setMetricsLoading] = useState(false);
  const [selectedChart, setSelectedChart] = useState('cpu');
  const [dataSource, setDataSource] = useState('agent'); // 'agent' | 'csp'

  useEffect(() => {
    if (!nsId || !mciId) return;
    loadVms();
  }, [nsId, mciId]);

  async function loadVms() {
    setLoading(true);
    try {
      const mciData = await getMci(nsId, mciId);
      const vmList = mciData.vm || [];
      setVms(vmList);
      // Load metrics right after we have VMs
      if (vmList.length > 0) {
        setMetricsLoading(true);
        if (dataSource === 'agent') await loadAgentData(vmList);
        else await loadCspData(vmList);
        setMetricsLoading(false);
      }
    } catch { setVms([]); }
    setLoading(false);
  }

  // Reload when dataSource changes
  useEffect(() => {
    if (vms.length === 0) return;
    setVmData({});
    setMetricsLoading(true);
    const load = dataSource === 'agent' ? loadAgentData : loadCspData;
    load(vms).finally(() => setMetricsLoading(false));
  }, [dataSource]);

  async function loadAgentData(vmList) {
    const data = {};
    for (const vm of vmList) {
      data[vm.id] = {};
    }
    await Promise.allSettled(
      vmList.map(async (vm) => {
        await Promise.allSettled(
          AGENT_METRICS.map(async (m) => {
            try {
              const res = await getMetricsByVM(nsId, mciId, vm.id, {
                measurement: m.measurement, range: '1h', groupTime: '1m',
                fields: [{ function: 'mean', field: m.field }],
              });
              data[vm.id][m.key] = { res, ...m };
            } catch (e) {
              data[vm.id][m.key] = { res: [], ...m };
            }
          })
        );
      })
    );
    setVmData({ ...data });
  }

  async function loadCspData(vmList) {
    const data = {};
    await Promise.allSettled(
      vmList.map(async (vm) => {
        if (!vm.connectionName || !vm.cspResourceName) return;
        const cspData = await getAllCspMetrics(vm.connectionName, vm.cspResourceName, '1');
        data[vm.id] = cspData;
      })
    );
    setVmData({ ...data });
  }

  if (loading) return <p className="text-sm text-gray-400 p-4">Loading VMs...</p>;
  if (vms.length === 0) return <p className="text-sm text-gray-400 p-4">No VMs found</p>;

  const hasCspVm = vms.some((vm) => isCspSupported(vm.connectionName));

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">MCI Overview — {mciId}</h2>
        <div className="flex items-center gap-3">
          {hasCspVm && (
            <div className="flex bg-gray-100 rounded-lg p-0.5 text-xs">
              <button onClick={() => setDataSource('agent')}
                className={`px-3 py-1 rounded-md ${dataSource === 'agent' ? 'bg-white shadow text-blue-600 font-medium' : 'text-gray-500'}`}>
                Agent
              </button>
              <button onClick={() => setDataSource('csp')}
                className={`px-3 py-1 rounded-md ${dataSource === 'csp' ? 'bg-white shadow text-blue-600 font-medium' : 'text-gray-500'}`}>
                CSP
              </button>
            </div>
          )}
          <span className="text-xs text-gray-400">{vms.length} VMs</span>
        </div>
      </div>

      {vms.map((vm) => (
        <VmCard
          key={vm.id}
          vm={vm}
          vmMetrics={vmData[vm.id] || {}}
          dataSource={dataSource}
          metricsLoading={metricsLoading}
          selectedChart={selectedChart}
          onSelectChart={setSelectedChart}
          onClickChart={() => navigate(`/monitoring/${nsId}/${mciId}/${vm.id}${dataSource === 'csp' ? '?source=csp' : ''}`)}
        />
      ))}
    </div>
  );
}

function VmCard({ vm, vmMetrics, dataSource, metricsLoading, selectedChart, onSelectChart, onClickChart }) {
  const cspSupported = isCspSupported(vm.connectionName);

  if (dataSource === 'csp') {
    return <CspVmCard vm={vm} metrics={vmMetrics} metricsLoading={metricsLoading} selectedChart={selectedChart} onSelectChart={onSelectChart} onClickChart={onClickChart} cspSupported={cspSupported} />;
  }
  return <AgentVmCard vm={vm} metrics={vmMetrics} metricsLoading={metricsLoading} selectedChart={selectedChart} onSelectChart={onSelectChart} onClickChart={onClickChart} cspSupported={cspSupported} />;
}

function AgentVmCard({ vm, metrics, metricsLoading, selectedChart, onSelectChart, onClickChart, cspSupported }) {
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
      <VmHeader vm={vm} showCspBadge={false} cspAvailable={cspSupported} />
      <div className="flex">
        <div className="flex flex-col justify-center gap-2 px-4 py-3 w-52 shrink-0 border-r">
          {gauges.map((g) => (
            <GaugeItem key={g.key} g={g} active={selectedChart === g.key} onClick={() => onSelectChart(g.key)} />
          ))}
        </div>
        <div className="flex-1 px-2 py-1 min-w-0 cursor-pointer" onClick={onClickChart}>
          {metricsLoading ? (
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm animate-pulse">Loading data...</div>
          ) : (
            <MetricChart title={activeMetric.label} series={chartSeries} height={220} measurement={activeMetric.measurement} metric={activeMetric.field} />
          )}
        </div>
      </div>
    </div>
  );
}

function CspVmCard({ vm, metrics, metricsLoading, selectedChart, onSelectChart, onClickChart, cspSupported }) {
  if (!cspSupported) {
    return (
      <div className="bg-white rounded-lg shadow">
        <VmHeader vm={vm} showCspBadge={true} cspAvailable={false} />
        <div className="p-8 text-center text-sm text-gray-400">CSP monitoring not supported for this provider</div>
      </div>
    );
  }

  const cspKeys = Object.keys(metrics);
  const activeKey = cspKeys.includes(selectedChart) ? selectedChart : (cspKeys[0] || 'cpu_usage');
  const activeData = metrics[activeKey];

  return (
    <div className="bg-white rounded-lg shadow">
      <VmHeader vm={vm} showCspBadge={true} cspAvailable={true} />
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
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm animate-pulse">Loading CSP data...</div>
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

function VmHeader({ vm, showCspBadge, cspAvailable }) {
  return (
    <div className="flex items-center justify-between px-4 py-3 border-b">
      <div className="flex items-center gap-3">
        <span className="font-semibold text-sm">{vm.name || vm.id}</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${vm.status === 'Running' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{vm.status || '-'}</span>
        {showCspBadge && cspAvailable && <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-600">CSP</span>}
        {cspAvailable && !showCspBadge && <span className="text-xs text-gray-400">(CSP available)</span>}
      </div>
      <div className="text-xs text-gray-400">{vm.connectionName} {vm.publicIP && <span className="ml-2 font-mono">{vm.publicIP}</span>}</div>
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
  const lastRow = m.values[m.values.length - 1];
  const timeIdx = (m.columns || []).indexOf('timestamp');
  const valIdx = timeIdx === 0 ? 1 : 0;
  return lastRow[valIdx] != null ? parseFloat(lastRow[valIdx]) : null;
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
