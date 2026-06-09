import { useState, useEffect, useCallback, Fragment } from 'react';
import { useParams } from 'react-router-dom';
import {
  getAnomalySettings, createAnomalySetting, deleteAnomalySetting, getAnomalyHistory,
  getAnomalyMeasurements, getAnomalyOptions,
  getPredictionHistory, runPrediction, getPredictionOptions,
  getServerErrorRecords, getServerErrorRecord, detectServerError, rerunServerErrorAnalysis,
} from '../api/insight';
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
        <span className="ml-auto self-center text-xs text-gray-400 pr-2">
          {nsId}/{infraId}{nodeId ? `/${nodeId}` : ''}
        </span>
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
    data: history.map((h) => ({ x: new Date(h.timestamp).getTime(), y: h.anomaly_score ?? parseFloat(h.value) })),
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
  const [scope, setScope] = useState(nodeId ? 'node' : 'infra');
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const measurements = options.measurements || [];
  const intervals = options.execution_intervals || [];

  async function submit(e) {
    e.preventDefault();
    if (!measurement || !interval) { setErr('Measurement and interval are required.'); return; }
    setBusy(true); setErr('');
    try {
      const body = {
        ns_id: nsId,
        infra_id: infraId,
        node_id: scope === 'node' ? nodeId : null,
        measurement,
        execution_interval: interval,
      };
      await createAnomalySetting(body);
      onCreated();
    } catch (e2) {
      setErr(e2.response?.data?.error_message || e2.response?.data?.rs_msg || e2.message);
    }
    setBusy(false);
  }

  return (
    <form onSubmit={submit} className="p-4 border-b bg-gray-50 space-y-3">
      <div className="grid grid-cols-3 gap-3">
        <div>
          <label className="block text-xs text-gray-600 mb-1">Target</label>
          <select value={scope} onChange={(e) => setScope(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm">
            <option value="infra">Infra ({infraId})</option>
            {nodeId && <option value="node">Node ({nodeId})</option>}
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
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState('');

  useEffect(() => {
    getPredictionOptions().then((o) => setOptions(o || {})).catch(() => {});
  }, []);

  async function loadHistory() {
    if (!measurement) return;
    setLoading(true); setMsg('');
    try {
      const data = await getPredictionHistory(nsId, infraId, nodeId, measurement);
      setHistory(data.values || []);
    } catch { setHistory([]); }
    setLoading(false);
  }

  async function handleRun() {
    if (!measurement || !range) { setMsg('Measurement and range are required.'); return; }
    setLoading(true); setMsg('');
    try {
      await runPrediction(nsId, infraId, nodeId, { measurement, prediction_range: range });
      setMsg('Prediction started. Loading history…');
      await loadHistory();
    } catch (e) {
      setMsg(e.response?.data?.error_message || e.response?.data?.rs_msg || e.message);
    }
    setLoading(false);
  }

  const chartSeries = history.length > 0 ? [{
    name: 'Prediction',
    data: history.map((h) => ({ x: new Date(h.timestamp).getTime(), y: parseFloat(h.value) })),
  }] : [];

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-4 py-3 border-b font-semibold text-sm">Prediction</div>
      <div className="p-4">
        <div className="flex gap-3 mb-2 flex-wrap items-end">
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
          <button onClick={handleRun} disabled={loading} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:opacity-50">
            Run Prediction
          </button>
          <button onClick={loadHistory} disabled={loading} className="px-4 py-1.5 bg-purple-600 text-white rounded text-sm hover:bg-purple-700 disabled:opacity-50">
            Load History
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
      setMsg('records 조회 실패: ' + (e.response?.data?.error_message || e.message));
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
      setMsg(`Detect 요청됨 (accepted=${res?.accepted}, ids=${(res?.analysis_ids || []).join(',') || '-'})`);
      await load();
    } catch (e) {
      setMsg('Detect 실패 (LLM 미설정 가능): ' + (e.response?.data?.error_message || e.response?.data?.rs_msg || e.message));
    }
    setBusy(false);
  }

  async function handleRerun(id) {
    setBusy(true); setMsg('');
    try {
      await rerunServerErrorAnalysis(id);
      setMsg(`#${id} rerun 요청됨`);
      await load();
    } catch (e) {
      setMsg('Rerun 실패 (LLM 미설정 가능): ' + (e.response?.data?.error_message || e.response?.data?.rs_msg || e.message));
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
          <span className="text-xs text-gray-400">LLM(OpenAI/ollama) 미설정 시 분석은 실패합니다.</span>
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
