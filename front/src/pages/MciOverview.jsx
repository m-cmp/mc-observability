import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getMci } from '../api/tumblebug';
import { getMetricsByVM } from '../api/monitoring';
import { getAllCspMetrics, CSP_METRICS, isCspSupported } from '../api/csp';
import { getClusters, getCluster } from '../api/k8s';
import MetricChart from '../components/MetricChart';
import ProviderBadge from '../components/ProviderBadge';

const AGENT_METRICS = [
  { key: 'cpu', measurement: 'cpu', field: 'usage_idle', label: 'CPU Used', unit: '%', invert: true },
  { key: 'mem', measurement: 'mem', field: 'used_percent', label: 'Memory Used', unit: '%' },
  { key: 'disk', measurement: 'disk', field: 'used_percent', label: 'Disk Used', unit: '%' },
];

export default function MciOverview() {
  const { nsId, mciId } = useParams();
  const navigate = useNavigate();
  const [vms, setVms] = useState([]);
  const [vmData, setVmData] = useState({});
  const [loading, setLoading] = useState(true);
  const [metricsLoading, setMetricsLoading] = useState(false);
  const [selectedChart, setSelectedChart] = useState('cpu');
  const [dataSource, setDataSource] = useState('agent');
  const [viewTab, setViewTab] = useState('vm'); // 'vm' | 'k8s'
  const [clusters, setClusters] = useState([]);
  const [clustersLoading, setClustersLoading] = useState(false);

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

  // Load K8s clusters when tab switches
  useEffect(() => {
    if (viewTab !== 'k8s' || vms.length === 0) return;
    setClustersLoading(true);
    const connNames = [...new Set(vms.map(v => v.connectionName).filter(Boolean))];
    Promise.allSettled(connNames.map(async (conn) => {
      const list = await getClusters(conn);
      const detailed = await Promise.allSettled(list.map(c => getCluster(conn, c.IId?.NameId)));
      return detailed.filter(r => r.status === 'fulfilled').map(r => ({ ...r.value, connectionName: conn }));
    })).then(results => {
      const all = results.filter(r => r.status === 'fulfilled').flatMap(r => r.value);
      setClusters(all);
    }).finally(() => setClustersLoading(false));
  }, [viewTab, vms]);

  if (loading) return <p className="text-sm text-gray-400 p-4">Loading VMs...</p>;
  if (vms.length === 0) return <p className="text-sm text-gray-400 p-4">No VMs found</p>;

  const hasCspVm = vms.some((vm) => isCspSupported(vm.connectionName));

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <h2 className="text-lg font-semibold">MCI Overview — {mciId}</h2>
          {/* VM / K8s tab */}
          <div className="flex bg-gray-100 rounded-lg p-0.5 text-xs">
            <button onClick={() => setViewTab('vm')}
              className={`px-3 py-1.5 rounded-md ${viewTab === 'vm' ? 'bg-white shadow text-gray-800 font-medium' : 'text-gray-500'}`}>
              VM
            </button>
            <button onClick={() => setViewTab('k8s')}
              className={`px-3 py-1.5 rounded-md ${viewTab === 'k8s' ? 'bg-white shadow text-gray-800 font-medium' : 'text-gray-500'}`}>
              K8s
            </button>
          </div>
        </div>
        <div className="flex items-center gap-3">
          {viewTab === 'vm' && hasCspVm && (
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
          <span className="text-xs text-gray-400">{viewTab === 'vm' ? `${vms.length} VMs` : `${clusters.length} Clusters`}</span>
        </div>
      </div>

      {/* K8s Tab */}
      {viewTab === 'k8s' && (
        clustersLoading ? (
          <div className="text-sm text-gray-400 animate-pulse p-4">Loading clusters...</div>
        ) : clusters.length === 0 ? (
          <div className="bg-white rounded-lg shadow p-8 text-center text-sm text-gray-400">No K8s clusters found</div>
        ) : clusters.map((cluster, i) => (
          <div key={i} className="bg-white rounded-lg shadow">
            <div className="flex items-center justify-between px-4 py-3 border-b">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-sm">{cluster.IId?.NameId}</span>
                <ProviderBadge connectionName={cluster.connectionName} />
                <span className={`text-xs px-2 py-0.5 rounded-full ${cluster.Status === 'Active' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{cluster.Status}</span>
              </div>
              <span className="text-xs text-gray-400">{cluster.connectionName}</span>
            </div>
            {(cluster.NodeGroupList || []).map((ng, j) => (
              <div key={j} className="px-4 py-3 border-b last:border-b-0">
                <div className="text-sm font-medium mb-2">NodeGroup: {ng.IId?.NameId}</div>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                  {(ng.Nodes || []).map((node, k) => (
                    <div key={k} className="flex items-center gap-2 text-sm bg-gray-50 rounded px-3 py-2">
                      <span className="w-2 h-2 rounded-full bg-green-500" />
                      <span className="font-mono text-xs">{node.NameId || node.SystemId || `node-${k}`}</span>
                    </div>
                  ))}
                </div>
              </div>
            ))}
          </div>
        ))
      )}

      {/* VM Tab */}
      {viewTab === 'vm' && vms.map((vm) => (
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
            <div className="flex items-center justify-center h-[220px] text-gray-400 text-sm animate-pulse">Loading API data...</div>
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
      <div className="flex items-center gap-2">
        <ProviderBadge connectionName={vm.connectionName} />
        <span className="font-semibold text-sm">{vm.name || vm.id}</span>
        <span className={`text-xs px-2 py-0.5 rounded-full ${vm.status === 'Running' ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{vm.status || '-'}</span>
        {showCspBadge && cspAvailable && <span className="text-xs px-2 py-0.5 rounded-full bg-blue-50 text-blue-600" title="CSP API Based">API</span>}
        {cspAvailable && !showCspBadge && <span className="text-xs text-gray-400" title="CSP API Based">(API available)</span>}
      </div>
      <div className="text-xs text-gray-400">{vm.publicIP && <span className="font-mono">{vm.publicIP}</span>}</div>
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
