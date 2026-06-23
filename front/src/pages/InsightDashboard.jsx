import { useState, useEffect, useCallback, Fragment } from 'react';
import { useParams } from 'react-router-dom';
import {
  getAnomalySettings, createAnomalySetting, deleteAnomalySetting, getAnomalyHistory,
  getAnomalyMeasurements, getAnomalyOptions,
  getPredictionHistory, runPrediction, getPredictionOptions,
  getServerErrorRecords, getServerErrorRecord, detectServerError, rerunServerErrorAnalysis,
} from '../api/insight';
import useScopeTargets, { loadScopeNodes } from '../hooks/useScopeTargets';
import MetricChart from '../components/MetricChart';

const TABS = ['Anomaly Detection', 'Prediction', 'Server Error Analysis'];

export default function InsightDashboard() {
  const { nsId, infraId, nodeId } = useParams();
  const [tab, setTab] = useState(0);

  return (
    <div className="space-y-4">
      <div className="flex gap-1 bg-white rounded-lg shadow px-2 py-1">
        {TABS.map((t, i) => (
          <button key={t} onClick={() => setTab(i)}
            className={`px-4 py-2 text-sm rounded ${tab === i ? 'bg-purple-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}>
            {t}
          </button>
        ))}
      </div>
      {tab === 0 && <AnomalyTab nsId={nsId} infraId={infraId} nodeId={nodeId} />}
      {tab === 1 && <PredictionTab nsId={nsId} infraId={infraId} nodeId={nodeId} />}
      {tab === 2 && <ServerErrorTab />}
    </div>
  );
}

/* ----------------------------- Anomaly ----------------------------- */
function AnomalyTab({ nsId, infraId, nodeId }) {
  const [settings, setSettings] = useState([]);
  const [options, setOptions] = useState({ measurements: [], execution_intervals: [] });
  const [measurements, setMeasurements] = useState([]);
  const [selectedMeasurement, setSelectedMeasurement] = useState('');
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [showCreate, setShowCreate] = useState(false);

  const loadSettings = useCallback(() => {
    getAnomalySettings().then((d) => setSettings(Array.isArray(d) ? d : [])).catch(() => setSettings([]));
  }, []);

  useEffect(() => {
    loadSettings();
    getAnomalyOptions().then((o) => setOptions(o || {})).catch(() => {});
    getAnomalyMeasurements().then((d) => setMeasurements(Array.isArray(d) ? d.map((m) => m.measurement || m) : [])).catch(() => {});
  }, [loadSettings]);

  async function loadHistory() {
    if (!selectedMeasurement) return;
    setLoading(true);
    try {
      const data = await getAnomalyHistory(nsId, infraId, nodeId, selectedMeasurement);
      setHistory(data.values || []);
    } catch { setHistory([]); }
    setLoading(false);
  }

  async function handleDelete(seq) {
    if (!confirm(`Delete anomaly setting #${seq}?`)) return;
    await deleteAnomalySetting(seq);
    loadSettings();
  }

  const chartSeries = history.length > 0 ? [{
    name: 'Anomaly Score',
    data: history
      .map((h) => ({ x: new Date(h.timestamp).getTime(), y: h.anomaly_score ?? (h.value == null ? null : parseFloat(h.value)) }))
      .filter((p) => p.y != null && !Number.isNaN(p.y)),
  }] : [];

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex justify-between items-center">
          <span className="font-semibold text-sm">Anomaly Detection Settings</span>
          <button onClick={() => setShowCreate(!showCreate)} className="text-xs bg-purple-600 text-white px-3 py-1 rounded hover:bg-purple-700">
            {showCreate ? 'Cancel' : '+ New Setting'}
          </button>
        </div>
        {showCreate && (
          <CreateAnomalyForm nsId={nsId} infraId={infraId} nodeId={nodeId} options={options}
            onCreated={() => { setShowCreate(false); loadSettings(); }} />
        )}
        <div className="p-4 overflow-auto">
          {settings.length === 0 ? <p className="text-sm text-gray-400">No settings configured</p> : (
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                <th className="px-3 py-2 border-b text-xs text-gray-500">NS / Infra / Node</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Measurement</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Interval</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Last Run</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Actions</th>
              </tr></thead>
              <tbody>
                {settings.map((s) => (
                  <tr key={s.seq} className="hover:bg-gray-50">
                    <td className="px-3 py-2 border-b">{s.ns_id}/{s.infra_id}/{s.node_id || '-'}</td>
                    <td className="px-3 py-2 border-b">{s.measurement}</td>
                    <td className="px-3 py-2 border-b">{s.execution_interval}</td>
                    <td className="px-3 py-2 border-b text-xs text-gray-500">{s.last_execution || '-'}</td>
                    <td className="px-3 py-2 border-b text-right">
                      <button onClick={() => handleDelete(s.seq)} className="text-xs text-red-500 hover:text-red-700">Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Anomaly Detection History</div>
        <div className="p-4">
          <div className="flex gap-3 mb-4">
            <select className="border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedMeasurement} onChange={(e) => setSelectedMeasurement(e.target.value)}>
              <option value="">Select Measurement</option>
              {(measurements.length ? measurements : options.measurements || []).map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
            <button onClick={loadHistory} disabled={loading} className="px-4 py-1.5 bg-purple-600 text-white rounded text-sm hover:bg-purple-700 disabled:opacity-50">
              {loading ? 'Loading...' : 'Load History'}
            </button>
          </div>
          <MetricChart title="Anomaly Score" series={chartSeries} height={240} chartType="line" />
        </div>
      </div>
    </div>
  );
}

function CreateAnomalyForm({ nsId, infraId, nodeId, options, onCreated }) {
  const [measurement, setMeasurement] = useState('');
  const [interval, setInterval] = useState('');
  // Scope: pick Infra/Cluster, then optionally a Node (empty = all nodes).
  const [infra, setInfra] = useState(infraId || '');
  const [node, setNode] = useState(nodeId || '');
  const { infras, clusters } = useScopeTargets(nsId);
  const [nodeList, setNodeList] = useState([]);
  const [nodesLoading, setNodesLoading] = useState(false);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const measurements = options.measurements || [];
  const intervals = options.execution_intervals || [];
  const isK8s = clusters.some((c) => c.id === infra);

  useEffect(() => {
    if (!nsId || !infra) { setNodeList([]); return; }
    let alive = true;
    setNodesLoading(true);
    loadScopeNodes(nsId, infra, isK8s)
      .then((ns) => { if (alive) setNodeList(ns); })
      .catch(() => { if (alive) setNodeList([]); })
      .finally(() => { if (alive) setNodesLoading(false); });
    return () => { alive = false; };
  }, [nsId, infra, isK8s]);

  async function submit(e) {
    e.preventDefault();
    if (!infra) { setErr('Infra is required.'); return; }
    if (!measurement || !interval) { setErr('Measurement and interval are required.'); return; }
    setBusy(true); setErr('');
    try {
      const body = {
        ns_id: nsId,
        infra_id: infra,
        node_id: node || null,
        measurement,
        execution_interval: interval,
      };
      await createAnomalySetting(body);
      onCreated();
    } catch (e2) {
      setErr(e2.response?.data?.detail || e2.response?.data?.error_message || e2.response?.data?.rs_msg || e2.message);
    }
    setBusy(false);
  }

  return (
    <form onSubmit={submit} className="p-4 border-b bg-gray-50 space-y-3">
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {/* Infra selector only when not already fixed by the path */}
        {!infraId && (
          <div>
            <label className="block text-xs text-gray-600 mb-1">Scope — Infra / Cluster</label>
            <select value={infra} onChange={(e) => { setInfra(e.target.value); setNode(''); }} className="w-full border rounded px-3 py-1.5 text-sm">
              <option value="">Select Infra / Cluster</option>
              {infras.length > 0 && (
                <optgroup label="VM Infra">
                  {infras.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
                </optgroup>
              )}
              {clusters.length > 0 && (
                <optgroup label="K8s Cluster">
                  {clusters.map((c) => <option key={c.id} value={c.id}>{c.name || c.id}</option>)}
                </optgroup>
              )}
            </select>
          </div>
        )}
        <div>
          <label className="block text-xs text-gray-600 mb-1">Scope — Node</label>
          <select value={node} onChange={(e) => setNode(e.target.value)} disabled={!infra} className="w-full border rounded px-3 py-1.5 text-sm disabled:bg-gray-100">
            <option value="">{nodesLoading ? 'Loading nodes…' : 'All nodes'}</option>
            {nodesLoading ? <option disabled>Loading nodes…</option> : nodeList.map((n) => <option key={n.id} value={n.id}>{n.name || n.id}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Measurement</label>
          <select value={measurement} onChange={(e) => setMeasurement(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm">
            <option value="">Select</option>
            {measurements.map((m) => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>
        <div>
          <label className="block text-xs text-gray-600 mb-1">Execution Interval</label>
          <select value={interval} onChange={(e) => setInterval(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm">
            <option value="">Select</option>
            {intervals.map((i) => <option key={i} value={i}>{i}</option>)}
          </select>
        </div>
      </div>
      {err && <p className="text-xs text-red-500">{err}</p>}
      <button type="submit" disabled={busy} className="bg-purple-600 text-white px-4 py-1.5 rounded text-sm hover:bg-purple-700 disabled:opacity-50">
        {busy ? 'Creating...' : 'Create'}
      </button>
    </form>
  );
}

/* ----------------------------- Prediction ----------------------------- */
// prediction_range is a duration string within the options' min~max bounds (e.g. 1h~2160h),
// NOT the option keys ("min"/"max"). Offer sensible presets.
const RANGE_PRESETS = [
  { value: '1h', label: '1 hour' },
  { value: '6h', label: '6 hours' },
  { value: '12h', label: '12 hours' },
  { value: '24h', label: '1 day' },
  { value: '72h', label: '3 days' },
  { value: '168h', label: '7 days' },
  { value: '720h', label: '30 days' },
];

function PredictionTab({ nsId, infraId, nodeId }) {
  const [options, setOptions] = useState({ measurements: [], prediction_ranges: {} });
  const [measurement, setMeasurement] = useState('');
  const [range, setRange] = useState('24h');
  const [history, setHistory] = useState([]);
  const [loadedMeasurement, setLoadedMeasurement] = useState('');
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState('');
  // Scope: pick Infra/Cluster, then optionally a Node (empty = all nodes).
  const [pInfra, setPInfra] = useState(infraId || '');
  const [pNode, setPNode] = useState(nodeId || '');
  const { infras, clusters } = useScopeTargets(nsId);
  const [nodeList, setNodeList] = useState([]);
  const [nodesLoading, setNodesLoading] = useState(false);
  const isK8s = clusters.some((c) => c.id === pInfra);

  useEffect(() => {
    getPredictionOptions().then((o) => setOptions(o || {})).catch(() => {});
  }, []);

  useEffect(() => {
    if (!nsId || !pInfra) { setNodeList([]); return; }
    let alive = true;
    setNodesLoading(true);
    loadScopeNodes(nsId, pInfra, isK8s)
      .then((ns) => { if (alive) setNodeList(ns); })
      .catch(() => { if (alive) setNodeList([]); })
      .finally(() => { if (alive) setNodesLoading(false); });
    return () => { alive = false; };
  }, [nsId, pInfra, isK8s]);

  async function loadHistory() {
    if (!pInfra) { setMsg('Select an Infra first.'); return; }
    if (!measurement) return;
    setLoading(true); setMsg('');
    try {
      const data = await getPredictionHistory(nsId, pInfra, pNode, measurement);
      setHistory(data.values || []);
      setLoadedMeasurement(measurement);
    } catch { setHistory([]); }
    setLoading(false);
  }

  async function handleRun() {
    if (!pInfra) { setMsg('Select an Infra first.'); return; }
    if (!measurement || !range) { setMsg('Measurement and range are required.'); return; }
    setLoading(true); setMsg('');
    try {
      await runPrediction(nsId, pInfra, pNode, { measurement, prediction_range: range });
      setMsg('Prediction started. Loading history…');
      await loadHistory();
    } catch (e) {
      setMsg(e.response?.data?.detail || e.response?.data?.error_message || e.response?.data?.rs_msg || e.message);
    }
    setLoading(false);
  }

  // Backend predicts the cpu measurement's `usage_idle` field. Show it as usage (100 - idle),
  // matching the Monitoring dashboard's "CPU Used" convention.
  const isCpu = loadedMeasurement === 'cpu';
  const chartSeries = history.length > 0 ? [{
    name: isCpu ? 'Predicted CPU Usage (%)' : 'Prediction',
    data: history
      .map((h) => {
        if (h.value == null) return { x: new Date(h.timestamp).getTime(), y: null };
        const v = parseFloat(h.value);
        return { x: new Date(h.timestamp).getTime(), y: isCpu ? 100 - v : v };
      })
      .filter((p) => p.y != null && !Number.isNaN(p.y)),
  }] : [];

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-4 py-3 border-b font-semibold text-sm">Prediction</div>
      <div className="p-4">
        <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded px-3 py-2 mb-3">
          Predictions become available about <strong>one day</strong> after the monitoring agent is installed
          (from the <strong>Config</strong> menu), once enough metric history has been collected.
        </p>
        <div className="flex gap-3 mb-2 flex-wrap items-end">
          {/* Infra selector only when not already fixed by the path */}
          {!infraId && (
            <div>
              <label className="block text-xs text-gray-600 mb-1">Scope — Infra / Cluster</label>
              <select className="border border-gray-300 rounded px-3 py-1.5 text-sm" value={pInfra} onChange={(e) => { setPInfra(e.target.value); setPNode(''); }}>
                <option value="">Select Infra / Cluster</option>
                {infras.length > 0 && (
                  <optgroup label="VM Infra">
                    {infras.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
                  </optgroup>
                )}
                {clusters.length > 0 && (
                  <optgroup label="K8s Cluster">
                    {clusters.map((c) => <option key={c.id} value={c.id}>{c.name || c.id}</option>)}
                  </optgroup>
                )}
              </select>
            </div>
          )}
          <div>
            <label className="block text-xs text-gray-600 mb-1">Scope — Node</label>
            <select className="border border-gray-300 rounded px-3 py-1.5 text-sm disabled:bg-gray-100" value={pNode} onChange={(e) => setPNode(e.target.value)} disabled={!pInfra}>
              <option value="">{nodesLoading ? 'Loading nodes…' : 'All nodes'}</option>
              {nodesLoading ? <option disabled>Loading nodes…</option> : nodeList.map((n) => <option key={n.id} value={n.id}>{n.name || n.id}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs text-gray-600 mb-1">Measurement</label>
            <select className="border border-gray-300 rounded px-3 py-1.5 text-sm" value={measurement} onChange={(e) => setMeasurement(e.target.value)}>
              <option value="">Select</option>
              {(options.measurements || []).map((m) => <option key={m} value={m}>{m}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs text-gray-600 mb-1">Prediction Range</label>
            <select className="border border-gray-300 rounded px-3 py-1.5 text-sm" value={range} onChange={(e) => setRange(e.target.value)}>
              {RANGE_PRESETS.map((r) => <option key={r.value} value={r.value}>{r.label}</option>)}
            </select>
          </div>
          <button onClick={handleRun} disabled={loading} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:opacity-50" title="Run a new prediction and store the result">
            Run Prediction
          </button>
          <button onClick={loadHistory} disabled={loading} className="px-4 py-1.5 bg-purple-600 text-white rounded text-sm hover:bg-purple-700 disabled:opacity-50" title="Load previously stored prediction (no re-run)">
            Load Saved
          </button>
        </div>
        {msg && <p className="text-xs text-gray-500 mb-2">{msg}</p>}
        <MetricChart title="Prediction" series={chartSeries} height={240} />
      </div>
    </div>
  );
}

/* ----------------------- Server Error Analysis ----------------------- */
const SE_STATUS = ['', 'PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'PARTIAL'];

function ServerErrorTab() {
  const [records, setRecords] = useState([]);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);
  const [detail, setDetail] = useState(null);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [limit, setLimit] = useState(20);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getServerErrorRecords({ status, size: 50 });
      setRecords(data.items || []);
    } catch (e) {
      setRecords([]);
      setMsg('Failed to load records: ' + (e.response?.data?.error_message || e.message));
    }
    setLoading(false);
  }, [status]);

  useEffect(() => { load(); }, [load]);

  async function openDetail(id) {
    if (selected === id) { setSelected(null); setDetail(null); return; }
    setSelected(id); setDetail(null);
    try { setDetail(await getServerErrorRecord(id)); } catch { setDetail({ error: 'load failed' }); }
  }

  async function handleDetect() {
    setBusy(true); setMsg('');
    try {
      const res = await detectServerError({ provider: 'openai', model_name: 'gpt-5-mini', limit: Number(limit) });
      setMsg(`Detect requested (accepted=${res?.accepted}, ids=${(res?.analysis_ids || []).join(",") || "-"})`);
      await load();
    } catch (e) {
      setMsg('Detect failed (LLM may be unconfigured): ' + (e.response?.data?.detail || e.response?.data?.error_message || e.response?.data?.rs_msg || e.message));
    }
    setBusy(false);
  }

  async function handleRerun(id) {
    setBusy(true); setMsg('');
    try {
      await rerunServerErrorAnalysis(id);
      setMsg(`#${id} rerun requested`);
      await load();
    } catch (e) {
      setMsg('Rerun failed (LLM may be unconfigured): ' + (e.response?.data?.detail || e.response?.data?.error_message || e.response?.data?.rs_msg || e.message));
    }
    setBusy(false);
  }

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex items-center gap-3">
          <span className="font-semibold text-sm">Server Error (5xx) Analysis</span>
          <select className="border rounded px-2 py-1 text-xs ml-auto" value={status} onChange={(e) => setStatus(e.target.value)}>
            {SE_STATUS.map((s) => <option key={s} value={s}>{s || 'All status'}</option>)}
          </select>
          <button onClick={load} className="text-xs text-gray-500 hover:text-gray-700">Refresh</button>
        </div>
        <div className="p-4 flex items-end gap-3 border-b bg-gray-50">
          <div>
            <label className="block text-xs text-gray-600 mb-1">Limit (max 5xx to analyze)</label>
            <input type="number" min={1} max={100} value={limit} onChange={(e) => setLimit(e.target.value)} className="border rounded px-3 py-1.5 text-sm w-28" />
          </div>
          <button onClick={handleDetect} disabled={busy} className="px-4 py-1.5 bg-purple-600 text-white rounded text-sm hover:bg-purple-700 disabled:opacity-50">
            {busy ? 'Working...' : 'Detect 5xx & Analyze'}
          </button>
          <span className="text-xs text-gray-400">Analysis requires an LLM (OpenAI/ollama) to be configured.</span>
        </div>
        {msg && <p className="text-xs text-gray-500 px-4 pt-2">{msg}</p>}
        <div className="p-4 overflow-auto">
          {loading ? <p className="text-sm text-gray-400">Loading...</p> : records.length === 0 ? (
            <p className="text-sm text-gray-400">No analysis records</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                <th className="px-3 py-2 border-b text-xs text-gray-500 w-6" />
                <th className="px-3 py-2 border-b text-xs text-gray-500">ID</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Status</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Trace ID</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Summary</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Created</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Actions</th>
              </tr></thead>
              <tbody>
                {records.map((r) => (
                  <Fragment key={r.id}>
                    <tr onClick={() => openDetail(r.id)} className={`cursor-pointer hover:bg-blue-50 ${selected === r.id ? 'bg-blue-100' : ''}`}>
                      <td className="px-3 py-2 border-b text-gray-400">{selected === r.id ? '▼' : '▶'}</td>
                      <td className="px-3 py-2 border-b">{r.id}</td>
                      <td className="px-3 py-2 border-b"><SeBadge status={r.status} /></td>
                      <td className="px-3 py-2 border-b font-mono text-xs text-gray-500">{r.trace_id ? r.trace_id.slice(0, 16) + '…' : '-'}</td>
                      <td className="px-3 py-2 border-b text-xs">{r.summary ? r.summary.slice(0, 60) : '-'}</td>
                      <td className="px-3 py-2 border-b text-xs text-gray-500">{fmt(r.created_at)}</td>
                      <td className="px-3 py-2 border-b text-right">
                        <button onClick={(e) => { e.stopPropagation(); handleRerun(r.id); }} className="text-xs text-blue-500 hover:text-blue-700">Rerun</button>
                      </td>
                    </tr>
                    {selected === r.id && (
                      <tr>
                        <td colSpan={7} className="border-b bg-gray-50 p-3">
                          {!detail ? <p className="text-sm text-gray-400 animate-pulse">Loading detail...</p> : (
                            <div className="space-y-2">
                              <div className="text-xs text-gray-500">Summary</div>
                              <p className="text-sm whitespace-pre-wrap">{detail.summary || '-'}</p>
                              <div className="text-xs text-gray-500 mt-2">Detail</div>
                              <pre className="text-xs bg-white border rounded p-2 overflow-auto max-h-80">{detail.detail ? JSON.stringify(detail.detail, null, 2) : '-'}</pre>
                            </div>
                          )}
                        </td>
                      </tr>
                    )}
                  </Fragment>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function SeBadge({ status }) {
  const s = (status || '').toUpperCase();
  const c = s === 'SUCCEEDED' ? 'bg-green-100 text-green-700'
    : s === 'RUNNING' ? 'bg-blue-100 text-blue-700'
    : s === 'PENDING' ? 'bg-yellow-100 text-yellow-700'
    : s === 'FAILED' ? 'bg-red-100 text-red-700'
    : s === 'PARTIAL' ? 'bg-orange-100 text-orange-700' : 'bg-gray-100 text-gray-500';
  return <span className={`text-xs px-2 py-0.5 rounded-full ${c}`}>{status || '-'}</span>;
}

function fmt(t) {
  if (!t) return '-';
  try { return new Date(t).toLocaleString(); } catch { return String(t); }
}
