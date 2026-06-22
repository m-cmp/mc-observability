import { useState, useEffect } from 'react';
import { getK8sClusters, getK8sAgentStatus } from '../api/k8sAgent';
import { getMetricsByNode } from '../api/monitoring';
import MetricChart from './MetricChart';
import ProviderBadge from './ProviderBadge';

// Host-agent (telegraf) metrics for K8s nodes, queried from InfluxDB by
// (ns_id, infra_id=clusterId, node_id=k8s node name) — same schema as VM agents.
// Mirrors the full set of telegraf inputs the agent collects (cpu/mem/disk/diskio/
// net/system/processes/swap) so the Agent tab shows varied graphs like the API tab.
const METRICS = [
  { key: 'cpu', measurement: 'cpu', field: 'usage_idle', label: 'CPU Used', unit: '%', invert: true },
  { key: 'cpu_user', measurement: 'cpu', field: 'usage_user', label: 'CPU User', unit: '%' },
  { key: 'cpu_system', measurement: 'cpu', field: 'usage_system', label: 'CPU System', unit: '%' },
  { key: 'cpu_iowait', measurement: 'cpu', field: 'usage_iowait', label: 'CPU IOWait', unit: '%' },
  { key: 'mem', measurement: 'mem', field: 'used_percent', label: 'Memory Used', unit: '%' },
  { key: 'swap', measurement: 'swap', field: 'used_percent', label: 'Swap Used', unit: '%' },
  { key: 'disk', measurement: 'disk', field: 'used_percent', label: 'Disk Used', unit: '%' },
  { key: 'diskio_read', measurement: 'diskio', field: 'read_bytes', label: 'Disk Read', unit: 'B' },
  { key: 'diskio_write', measurement: 'diskio', field: 'write_bytes', label: 'Disk Write', unit: 'B' },
  { key: 'net_recv', measurement: 'net', field: 'bytes_recv', label: 'Net Recv', unit: 'B' },
  { key: 'net_sent', measurement: 'net', field: 'bytes_sent', label: 'Net Sent', unit: 'B' },
  { key: 'load1', measurement: 'system', field: 'load1', label: 'Load (1m)', unit: '' },
  { key: 'procs', measurement: 'processes', field: 'total', label: 'Processes', unit: '' },
];

function fmtValue(val, unit) {
  if (val == null) return '-';
  if (unit === 'B') {
    const u = ['B', 'KB', 'MB', 'GB', 'TB'];
    let n = val, i = 0;
    while (n >= 1024 && i < u.length - 1) { n /= 1024; i++; }
    return n.toFixed(n >= 100 || i === 0 ? 0 : 1) + ' ' + u[i];
  }
  if (unit === '%') return val.toFixed(1) + '%';
  if (unit === '') return Number.isInteger(val) ? String(val) : val.toFixed(2);
  return val.toFixed(1) + ' ' + unit;
}

function getLastValue(res) {
  if (!res || res.length === 0) return null;
  const m = res[0];
  if (!m.values || m.values.length === 0) return null;
  const tIdx = (m.columns || []).indexOf('timestamp') < 0 ? 0 : (m.columns || []).indexOf('timestamp');
  const vIdx = tIdx === 0 ? 1 : 0;
  let bestTs = -Infinity, best = null;
  for (const row of m.values) {
    if (row[vIdx] == null) continue;
    const raw = row[tIdx];
    const ts = typeof raw === 'string' ? new Date(raw).getTime() : Number(raw);
    if (ts > bestTs) { bestTs = ts; best = parseFloat(row[vIdx]); }
  }
  return best;
}

function toSeries(res, name, invert) {
  if (!res || res.length === 0) return [];
  return res.map((m) => {
    const tIdx = (m.columns || []).indexOf('timestamp');
    const vIdx = tIdx === 0 ? 1 : 0;
    return {
      name: name || 'value',
      data: (m.values || []).filter((row) => row[vIdx] != null).map((row) => ({
        x: typeof row[tIdx] === 'string' ? new Date(row[tIdx]).getTime() : row[tIdx],
        y: invert ? 100 - parseFloat(row[vIdx]) : parseFloat(row[vIdx]),
      })),
    };
  });
}

export default function K8sAgentMonitor({ nsId }) {
  const [clusters, setClusters] = useState([]);
  const [nodesMap, setNodesMap] = useState({}); // clusterId -> [nodeName]
  const [data, setData] = useState({}); // `${clusterId}/${node}` -> { cpu:{res}, ... }
  const [selectedChart, setSelectedChart] = useState('cpu');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!nsId) { setClusters([]); setLoading(false); return; }
    let alive = true;
    setLoading(true);
    setData({}); setNodesMap({});
    getK8sClusters(nsId)
      .then(async (cs) => {
        const arr = Array.isArray(cs) ? cs : [];
        if (!alive) return;
        setClusters(arr);
        for (const c of arr) {
          let nodes = [];
          try {
            const st = await getK8sAgentStatus(nsId, c.id);
            nodes = (Array.isArray(st) ? st : []).map((n) => n.node);
          } catch { nodes = []; }
          if (!alive) return;
          setNodesMap((m) => ({ ...m, [c.id]: nodes }));
          for (const node of nodes) {
            METRICS.forEach(async (mt) => {
              try {
                const res = await getMetricsByNode(nsId, c.id, node, {
                  measurement: mt.measurement, range: '1h', groupTime: '1m',
                  fields: [{ function: 'mean', field: mt.field }],
                });
                if (!alive) return;
                setData((d) => ({ ...d, [`${c.id}/${node}`]: { ...(d[`${c.id}/${node}`] || {}), [mt.key]: { res } } }));
              } catch { /* node may have no data yet */ }
            });
          }
        }
      })
      .catch(() => alive && setClusters([]))
      .finally(() => alive && setLoading(false));
    return () => { alive = false; };
  }, [nsId]);

  if (loading) return <div className="text-sm text-gray-400 animate-pulse p-4">Loading K8s agent metrics…</div>;
  if (clusters.length === 0) return <div className="bg-white rounded-lg shadow p-8 text-center text-sm text-gray-400">No K8s clusters in this namespace</div>;

  return (
    <>
      {clusters.map((c) => {
        const nodes = nodesMap[c.id] || [];
        return (
          <div key={c.id} className="bg-white rounded-lg shadow">
            <div className="px-4 py-3 border-b flex items-center gap-3">
              <ProviderBadge connectionName={c.connectionName} />
              <span className="text-[10px] uppercase tracking-wide text-gray-400 font-medium">Cluster</span>
              <span className="font-semibold text-sm truncate">{c.name || c.id}</span>
              <span className={`text-xs px-2 py-0.5 rounded-full ${(c.status || '').includes('Active') ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{c.status || '-'}</span>
              <span className="text-xs text-gray-400 ml-auto">{nodes.length} agent node(s)</span>
            </div>
            <div className="p-3 space-y-3">
              {nodes.length === 0
                ? <div className="p-6 text-center text-xs text-gray-400">No host agent installed — install from the Config menu.</div>
                : nodes.map((node) => (
                  <NodeCard key={node} node={node} metrics={data[`${c.id}/${node}`] || {}}
                    selectedChart={selectedChart} onSelectChart={setSelectedChart} />
                ))}
            </div>
          </div>
        );
      })}
    </>
  );
}

function NodeCard({ node, metrics, selectedChart, onSelectChart }) {
  const gauges = METRICS.map((m) => {
    const d = metrics[m.key];
    const last = d?.res ? getLastValue(d.res) : null;
    const val = last != null ? (m.invert ? 100 - last : last) : null;
    return { ...m, display: fmtValue(val, m.unit) };
  });
  const active = METRICS.find((m) => m.key === selectedChart) || METRICS[0];
  const cd = metrics[active.key];
  const series = cd?.res ? toSeries(cd.res, active.label, active.invert) : [];
  return (
    <div className="bg-white rounded-lg border">
      <div className="px-4 py-2 border-b">
        <span className="font-mono text-sm text-gray-700">{node}</span>
      </div>
      <div className="flex">
        <div className="flex flex-col justify-center gap-2 px-4 py-3 w-52 shrink-0 border-r max-h-[360px] overflow-auto">
          {gauges.map((g) => (
            <button key={g.key} onClick={() => onSelectChart(g.key)}
              className={`text-left px-2 py-1.5 rounded ${selectedChart === g.key ? 'bg-blue-50 ring-1 ring-blue-200' : 'hover:bg-gray-50'}`}>
              <div className="text-xs text-gray-500">{g.label}</div>
              <div className="text-sm font-semibold">{g.display}</div>
            </button>
          ))}
        </div>
        <div className="flex-1 px-2 py-1 min-w-0">
          {cd?.res ? (
            <MetricChart title={`${active.label}${active.unit ? ` (${active.unit})` : ''}`} series={series} height={200} measurement={active.measurement} metric={active.field} />
          ) : (
            <div className="flex items-center justify-center h-[200px] text-gray-400 text-sm animate-pulse">Loading {active.label}…</div>
          )}
        </div>
      </div>
    </div>
  );
}
