import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getK8sClusters, getK8sAgentStatus } from '../api/k8sAgent';
import { getMetricsByNode } from '../api/monitoring';
import useBasePath from '../hooks/useBasePath';
import MetricChart from './MetricChart';
import ProviderBadge from './ProviderBadge';

// Host-agent (telegraf) metrics for K8s nodes, queried from InfluxDB by
// (ns_id, infra_id=clusterId, node_id=k8s node name) — same schema as VM agents.
// Compact overview set (cpu/mem/disk/diskio/net) — click a chart to open the
// node's full monitoring detail (same as VM nodes).
const METRICS = [
  { key: 'cpu', measurement: 'cpu', field: 'usage_idle', label: 'CPU Used', unit: '%', invert: true },
  { key: 'mem', measurement: 'mem', field: 'used_percent', label: 'Memory Used', unit: '%' },
  { key: 'disk', measurement: 'disk', field: 'used_percent', label: 'Disk Used', unit: '%' },
  { key: 'diskio', measurement: 'diskio', field: 'read_bytes', label: 'Disk IO', unit: 'B' },
  { key: 'net_recv', measurement: 'net', field: 'bytes_recv', label: 'Net Recv', unit: 'B' },
  { key: 'net_sent', measurement: 'net', field: 'bytes_sent', label: 'Net Sent', unit: 'B' },
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
  const navigate = useNavigate();
  const base = useBasePath();
  const [clusters, setClusters] = useState([]);
  const [nodesMap, setNodesMap] = useState({}); // clusterId -> [{node, running}]
  const [statusLoaded, setStatusLoaded] = useState({}); // clusterId -> bool (agent status fetched)
  const [data, setData] = useState({}); // `${clusterId}/${node}` -> { cpu:{res}, ... }
  const [selectedChart, setSelectedChart] = useState('cpu');
  const [loading, setLoading] = useState(true);
  // Tell "backend unreachable" apart from "genuinely no clusters" so a failed
  // fetch shows an error + Retry instead of a misleading empty state.
  const [error, setError] = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const retry = () => setReloadKey((k) => k + 1);

  useEffect(() => {
    if (!nsId) { setClusters([]); setLoading(false); return; }
    let alive = true;
    setLoading(true);
    setError(null);
    setData({}); setNodesMap({}); setStatusLoaded({});
    getK8sClusters(nsId)
      .then(async (cs) => {
        const arr = Array.isArray(cs) ? cs : [];
        if (!alive) return;
        setClusters(arr);
        for (const c of arr) {
          let statuses = [];
          try {
            const st = await getK8sAgentStatus(nsId, c.id);
            statuses = Array.isArray(st) ? st : [];
          } catch { statuses = []; }
          if (!alive) return;
          setNodesMap((m) => ({ ...m, [c.id]: statuses }));
          setStatusLoaded((m) => ({ ...m, [c.id]: true }));
          // Only nodes with the agent actually running have metrics to load.
          for (const s of statuses.filter((n) => n.running)) {
            const node = s.node;
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
      .catch(() => {
        if (!alive) return;
        // getK8sClusters threw → backend unreachable, not an empty namespace.
        setClusters([]);
        setError('Failed to load K8s clusters from the monitoring backend.');
      })
      .finally(() => alive && setLoading(false));
    return () => { alive = false; };
  }, [nsId, reloadKey]);

  function openNode(clusterId, node) {
    navigate(`${base}/monitoring/${nsId}/${clusterId}/${node}?source=agent&k8s=1`);
  }

  if (loading) return <div className="text-sm text-gray-400 animate-pulse p-4">Loading K8s agent metrics…</div>;
  if (error) return (
    <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center justify-between gap-3">
      <div className="flex items-start gap-2 text-sm text-red-700 min-w-0">
        <span aria-hidden className="mt-0.5">⚠</span>
        <span className="min-w-0">
          Couldn't reach the monitoring backend, so K8s data may be missing.
          <span className="block text-xs text-red-500 mt-0.5 break-words">{error}</span>
        </span>
      </div>
      <button onClick={retry} className="text-xs px-3 py-1.5 rounded-md bg-red-600 text-white hover:bg-red-700 shrink-0">Retry</button>
    </div>
  );
  if (clusters.length === 0) return <div className="bg-white rounded-lg shadow p-8 text-center text-sm text-gray-400">No K8s clusters in this namespace</div>;

  return (
    <>
      {clusters.map((c) => {
        const statuses = nodesMap[c.id] || [];
        const reporting = statuses.filter((n) => n.running);
        const idle = statuses.filter((n) => !n.running); // off or agent not reporting
        const loaded = statusLoaded[c.id];
        return (
          <div key={c.id} className="bg-white rounded-lg shadow">
            <div className="px-4 py-3 border-b flex items-center gap-3">
              <ProviderBadge connectionName={c.connectionName} />
              <span className="text-[10px] uppercase tracking-wide text-gray-400 font-medium">Cluster</span>
              <span className="font-semibold text-sm truncate">{c.name || c.id}</span>
              <span className={`text-xs px-2 py-0.5 rounded-full ${(c.status || '').includes('Active') ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>{c.status || '-'}</span>
              <span className="text-xs text-gray-400 ml-auto">
                {!loaded ? 'checking agents…' : `${reporting.length}/${statuses.length} node(s) reporting`}
              </span>
            </div>
            <div className="p-3 space-y-3">
              {!loaded
                ? <div className="p-6 text-center text-xs text-gray-400 animate-pulse">Checking agent status…</div>
                : statuses.length === 0
                ? <div className="p-6 text-center text-xs text-gray-400">No nodes found for this cluster.</div>
                : (
                  <>
                    {reporting.map((s) => (
                      <NodeCard key={s.node} node={s.node} metrics={data[`${c.id}/${s.node}`] || {}}
                        selectedChart={selectedChart} onSelectChart={setSelectedChart}
                        onOpen={() => openNode(c.id, s.node)} />
                    ))}
                    {idle.length > 0 && (
                      <div className="rounded-lg border divide-y">
                        {idle.map((s) => <NodeStatusRow key={s.node} s={s} />)}
                      </div>
                    )}
                  </>
                )}
            </div>
          </div>
        );
      })}
    </>
  );
}

function fmtLastSeen(iso) {
  if (!iso) return null;
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return null;
  const d = new Date(t);
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

// Compact row for nodes that have no live metrics (powered off, or agent not reporting).
function NodeStatusRow({ s }) {
  const powered = String(s.powerState || '').toUpperCase() === 'RUNNING';
  const last = fmtLastSeen(s.lastSeen);
  let agentText, agentCls;
  if (s.running) { agentText = 'reporting'; agentCls = 'text-green-600'; }
  else if (s.installed) { agentText = last ? `agent installed · last seen ${last}` : 'agent installed'; agentCls = 'text-amber-600'; }
  else { agentText = 'agent not installed'; agentCls = 'text-gray-400'; }
  return (
    <div className="flex items-center gap-3 px-4 py-2.5">
      <span className={`font-mono text-sm ${s.placeholder ? 'text-gray-400 italic' : 'text-gray-700'}`}>{s.node}</span>
      <span className={`text-[11px] px-2 py-0.5 rounded-full ${powered ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
        {powered ? 'Running' : 'Stopped'}
      </span>
      <span className={`text-xs ml-auto ${agentCls}`}>{agentText}</span>
    </div>
  );
}

function NodeCard({ node, metrics, selectedChart, onSelectChart, onOpen }) {
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
      <div className="px-4 py-2 border-b flex items-center justify-between">
        <span className="font-mono text-sm text-gray-700">{node}</span>
        <button onClick={onOpen} className="text-xs text-blue-600 hover:underline">Detail →</button>
      </div>
      <div className="flex">
        <div className="flex flex-col justify-center gap-2 px-4 py-3 w-52 shrink-0 border-r">
          {gauges.map((g) => (
            <button key={g.key} onClick={() => onSelectChart(g.key)}
              className={`text-left px-2 py-1.5 rounded ${selectedChart === g.key ? 'bg-blue-50 ring-1 ring-blue-200' : 'hover:bg-gray-50'}`}>
              <div className="text-xs text-gray-500">{g.label}</div>
              <div className="text-sm font-semibold">{g.display}</div>
            </button>
          ))}
        </div>
        <div className="flex-1 px-2 py-1 min-w-0 cursor-pointer" onClick={onOpen} title="Open node detail">
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
