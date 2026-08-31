import { useState, useEffect, useCallback, useRef, Fragment } from 'react';
import { useParams } from 'react-router-dom';
import {
  getAnomalySettings, createAnomalySetting, deleteAnomalySetting, getAnomalyHistory,
  getAnomalyMeasurements, getAnomalyOptions,
  getPredictionHistory, runPrediction, getPredictionOptions,
  getRcaRecords, queryRca,
  getRcaSchedules, createRcaSchedule, updateRcaSchedule, deleteRcaSchedule, getRcaRecord,
  getLlmConnections, createLlmConnection, updateLlmConnection, setDefaultLlmConnection,
  deleteLlmConnection, getLlmConnectionModels,
} from '../api/insight';
import useScopeTargets, { loadScopeNodes } from '../hooks/useScopeTargets';
import MetricChart from '../components/MetricChart';
import { formatLocalTime, toEpochMillis } from '../utils/time';
import { apiError } from '../utils/error';
import { buildRcaRequest, formatRcaDuration, getRcaRecordView } from '../utils/rca';

const TABS = ['Anomaly Detection', 'Prediction', 'RCA'];

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
      {tab === 2 && <RcaTab nsId={nsId} infraId={infraId} nodeId={nodeId} />}
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

  // History target selector: default to the route scope, but let the user pick an
  // Infra/Cluster + Node so NS-level views don't query infra "undefined".
  const [histInfra, setHistInfra] = useState(infraId || '');
  const [histNode, setHistNode] = useState(nodeId || '');
  const { infras, clusters, loading: scopeLoading } = useScopeTargets(nsId);
  const [nodeList, setNodeList] = useState([]);
  const [nodesLoading, setNodesLoading] = useState(false);
  const isK8s = clusters.some((c) => c.id === histInfra);

  useEffect(() => {
    if (!nsId || !histInfra) { setNodeList([]); return; }
    let alive = true;
    setNodesLoading(true);
    loadScopeNodes(nsId, histInfra, isK8s)
      .then((ns) => { if (alive) setNodeList(ns); })
      .catch(() => { if (alive) setNodeList([]); })
      .finally(() => { if (alive) setNodesLoading(false); });
    return () => { alive = false; };
  }, [nsId, histInfra, isK8s]);

  const loadSettings = useCallback(() => {
    getAnomalySettings().then((d) => setSettings(Array.isArray(d) ? d : [])).catch(() => setSettings([]));
  }, []);

  useEffect(() => {
    loadSettings();
    getAnomalyOptions().then((o) => setOptions(o || {})).catch(() => {});
    getAnomalyMeasurements().then((d) => setMeasurements(Array.isArray(d) ? d.map((m) => m.measurement || m) : [])).catch(() => {});
  }, [loadSettings]);

  async function loadHistory() {
    if (!histInfra || !selectedMeasurement) return;
    setLoading(true);
    try {
      const data = await getAnomalyHistory(nsId, histInfra, histNode || undefined, selectedMeasurement);
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
      .map((h) => ({ x: toEpochMillis(h.timestamp), y: h.anomaly_score ?? (h.value == null ? null : parseFloat(h.value)) }))
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
          <div className="flex flex-wrap gap-3 mb-4 items-end">
            {/* Infra/Cluster selector (hidden when the route already fixes the Infra) */}
            {!infraId && (
              <div>
                <label className="block text-xs text-gray-600 mb-1">Infra / Cluster</label>
                <select value={histInfra} onChange={(e) => { setHistInfra(e.target.value); setHistNode(''); }} className="border border-gray-300 rounded px-3 py-1.5 text-sm">
                  <option value="">{scopeLoading ? 'Loading…' : 'Select Infra / Cluster'}</option>
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
              <label className="block text-xs text-gray-600 mb-1">Node</label>
              <select value={histNode} onChange={(e) => setHistNode(e.target.value)} disabled={!histInfra} className="border border-gray-300 rounded px-3 py-1.5 text-sm disabled:bg-gray-100">
                <option value="">{nodesLoading ? 'Loading nodes…' : 'All nodes'}</option>
                {!nodesLoading && nodeList.map((n) => <option key={n.id} value={n.id}>{n.name || n.id}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-xs text-gray-600 mb-1">Measurement</label>
              <select className="border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedMeasurement} onChange={(e) => setSelectedMeasurement(e.target.value)}>
                <option value="">Select Measurement</option>
                {(measurements.length ? measurements : options.measurements || []).map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <button onClick={loadHistory} disabled={loading || !histInfra || !selectedMeasurement} className="px-4 py-1.5 bg-purple-600 text-white rounded text-sm hover:bg-purple-700 disabled:opacity-50">
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
  const { infras, clusters, loading: scopeLoading } = useScopeTargets(nsId);
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
              <option value="">{scopeLoading ? 'Loading…' : 'Select Infra / Cluster'}</option>
              {scopeLoading && <option disabled>Loading infras / clusters…</option>}
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
  const { infras, clusters, loading: scopeLoading } = useScopeTargets(nsId);
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
        if (h.value == null) return { x: toEpochMillis(h.timestamp), y: null };
        const v = parseFloat(h.value);
        return { x: toEpochMillis(h.timestamp), y: isCpu ? 100 - v : v };
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
                <option value="">{scopeLoading ? 'Loading…' : 'Select Infra / Cluster'}</option>
                {scopeLoading && <option disabled>Loading infras / clusters…</option>}
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

/* ----------------------- RCA ----------------------- */
const SE_STATUS = ['', 'PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'PARTIAL'];

// Record table layout. The detail row spans all of them, so adding/removing a column
// here is the only edit needed.
const RCA_COLUMNS = [
  { label: '', width: 'w-8' },
  { label: 'State / Risk', width: 'w-32' },
  { label: 'Target', width: 'w-52' },
  { label: 'Probable cause', width: '' },
  { label: 'Confidence', width: 'w-32' },
  { label: 'Started', width: 'w-36' },
];

const LLM_COLUMNS = [
  { label: 'Name' }, { label: 'Provider' }, { label: 'Endpoint' }, { label: 'Default Model' },
  { label: 'Default' }, { label: 'Status' }, { label: 'Actions', className: 'text-right' },
];

// Connection the analysis form should start on: the default one, else any enabled one.
function preferredConnectionId(connections) {
  const enabled = connections.filter((item) => item.enabled);
  const chosen = enabled.find((item) => item.is_default) || enabled[0];
  return chosen ? String(chosen.id) : '';
}

function RcaTab({ nsId, infraId, nodeId }) {
  const [records, setRecords] = useState([]);
  const [status, setStatus] = useState('');
  const [listFrom, setListFrom] = useState('');
  const [listTo, setListTo] = useState('');
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [selected, setSelected] = useState(null);
  const [busy, setBusy] = useState(false);
  const [msg, setMsg] = useState('');
  const [query, setQuery] = useState('');
  const [traceId, setTraceId] = useState('');
  const [timeStart, setTimeStart] = useState('');
  const [timeEnd, setTimeEnd] = useState('');
  const [serviceName, setServiceName] = useState('');
  const [endpoint, setEndpoint] = useState('');
  const [statusCode, setStatusCode] = useState('');
  const [measurement, setMeasurement] = useState('');
  const [additionalFilters, setAdditionalFilters] = useState([{ key: '', value: '' }]);
  const [llmConnections, setLlmConnections] = useState([]);
  const [connectionId, setConnectionId] = useState('');
  const [connectionModels, setConnectionModels] = useState([]);
  const [modelName, setModelName] = useState('');
  const [schedules, setSchedules] = useState([]);
  const [scheduleName, setScheduleName] = useState('');
  const [scheduleInterval, setScheduleInterval] = useState(15);
  const [scheduleBusyId, setScheduleBusyId] = useState(null);
  // What the form produces: one analysis now, a repeating schedule, or a server-error watch.
  const [mode, setMode] = useState('once');
  // Bumped on unmount so a poll loop that outlives the page stops touching state.
  const pollToken = useRef(0);
  useEffect(() => () => { pollToken.current += 1; }, []);
  const [deletingId, setDeletingId] = useState(null);
  const [defaultingId, setDefaultingId] = useState(null);
  const [editingId, setEditingId] = useState(null);
  const [showConnectionForm, setShowConnectionForm] = useState(false);
  const [showAnalysisForm, setShowAnalysisForm] = useState(false);
  const enabledConnections = llmConnections.filter((connection) => connection.enabled);
  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  // Every connection mutation re-reads the list: the server owns which one is default
  // and whether an edit left it enabled, so local patching would drift.
  const loadConnections = useCallback(async () => {
    const connections = await getLlmConnections();
    setLlmConnections(connections);
    setConnectionId((current) => (
      connections.some((item) => String(item.id) === current && item.enabled)
        ? current
        : preferredConnectionId(connections)
    ));
    return connections;
  }, []);

  useEffect(() => {
    loadConnections().catch(() => { /* request will surface any config error */ });
  }, [loadConnections]);

  useEffect(() => {
    const connection = llmConnections.find((item) => String(item.id) === connectionId && item.enabled);
    if (!connection) {
      setConnectionModels([]);
      setModelName('');
      return undefined;
    }
    let active = true;
    setModelName(connection.default_model || '');
    getLlmConnectionModels(connection.id)
      .then((data) => {
        if (!active) return;
        const models = data.models || [];
        const options = connection.default_model && !models.includes(connection.default_model)
          ? [connection.default_model, ...models]
          : models;
        setConnectionModels(options);
        setModelName((current) => (
          options.includes(current) ? current : connection.default_model || options[0] || ''
        ));
      })
      .catch(() => {
        if (active) setConnectionModels(connection.default_model ? [connection.default_model] : []);
      });
    return () => { active = false; };
  }, [connectionId, llmConnections]);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getRcaRecords({
        status,
        from: listFrom ? new Date(listFrom).toISOString() : undefined,
        to: listTo ? new Date(listTo).toISOString() : undefined,
        page,
        size: pageSize,
      });
      setRecords(data.items || []);
      setTotal(data.total || 0);
    } catch (e) {
      setRecords([]);
      setTotal(0);
      setMsg('Failed to load records: ' + apiError(e));
    }
    setLoading(false);
  }, [status, listFrom, listTo, page, pageSize]);

  useEffect(() => { load(); }, [load]);

  function openDetail(record) {
    setSelected((current) => current === record.id ? null : record.id);
  }

  async function handleDeleteConnection(connection) {
    if (!confirm(`Delete LLM connection "${connection.name}"?`)) return;
    setDeletingId(connection.id);
    setMsg('');
    try {
      await deleteLlmConnection(connection.id);
      if (editingId === connection.id) setEditingId(null);
      await loadConnections();
      setMsg(`Connection "${connection.name}" deleted.`);
    } catch (e) {
      // 409 when the connection is the default one or still referenced by sessions.
      setMsg('Delete failed: ' + apiError(e));
    } finally {
      setDeletingId(null);
    }
  }

  async function handleSetDefault(connection) {
    setDefaultingId(connection.id);
    setMsg('');
    try {
      await setDefaultLlmConnection(connection.id, connection.default_model);
      await loadConnections();
      setMsg(`"${connection.name}" is now the default connection.`);
    } catch (e) {
      // 409 when the connection is disabled.
      setMsg('Set default failed: ' + apiError(e));
    } finally {
      setDefaultingId(null);
    }
  }

  const loadSchedules = useCallback(async () => {
    setSchedules(await getRcaSchedules());
  }, []);

  useEffect(() => {
    loadSchedules().catch(() => { /* surfaced when a schedule action runs */ });
  }, [loadSchedules]);

  // Schedules and watches are the same object (POST /rca/schedules); a watch adds
  // trigger="server_error" and needs no question, time range or status code.
  async function handleSaveSchedule() {
    let request;
    try {
      // No timeStart/timeEnd: a schedule's window comes from the slot being run, and the
      // API rejects a stored time_range outright.
      request = mode === 'watch'
        ? buildRcaRequest({ connectionId, modelName, serviceName, measurement, filters: additionalFilters }, { nsId, infraId, nodeId })
        : buildRcaRequest({
          query, traceId, connectionId, modelName,
          serviceName, endpoint, statusCode, measurement,
          filters: additionalFilters,
        }, { nsId, infraId, nodeId });
    } catch (e) {
      setMsg(e.message);
      return;
    }
    setBusy(true);
    setMsg('');
    try {
      await createRcaSchedule({
        name: scheduleName.trim(),
        enabled: true,
        interval_minutes: Number(scheduleInterval),
        ...(mode === 'watch' ? { trigger: 'server_error' } : {}),
        request,
      });
      setScheduleName('');
      setShowAnalysisForm(false);
      await loadSchedules();
      setMsg(mode === 'watch'
        ? `Watch "${scheduleName.trim()}" saved. Every ${scheduleInterval} minutes it checks for server errors and analyses them when found.`
        : `Schedule "${scheduleName.trim()}" saved. It runs every ${scheduleInterval} minutes.`);
    } catch (e) {
      setMsg('Saving failed: ' + apiError(e));
    } finally {
      setBusy(false);
    }
  }

  function handleSubmit() {
    if (mode === 'once') return handleAnalyze();
    return handleSaveSchedule();
  }

  async function handleScheduleAction(schedule, body) {
    setScheduleBusyId(schedule.id);
    setMsg('');
    try {
      if (body) await updateRcaSchedule(schedule.id, body);
      else await deleteRcaSchedule(schedule.id);
      await loadSchedules();
    } catch (e) {
      // 409 while the analysis is still running, 404 once it is gone.
      setMsg('Schedule update failed: ' + apiError(e));
    } finally {
      setScheduleBusyId(null);
    }
  }

  const RUNNING_STATES = ['PENDING', 'RUNNING'];

  // POST /rca/query answers at once with the record; the analysis runs on the server.
  // Poll the record until it settles, then open it.
  async function pollAnalysis(id) {
    const token = pollToken.current;
    const startedAt = Date.now();
    try {
      for (;;) {
        await new Promise((resolve) => setTimeout(resolve, 5000));
        if (pollToken.current !== token) return;
        const record = await getRcaRecord(id);
        if (!RUNNING_STATES.includes(record?.status)) {
          setMsg(`Analysis #${id} ${String(record?.status || '').toLowerCase()}`);
          setSelected(id);
          if (page === 1) await load();
          else setPage(1);
          return;
        }
        setMsg(`Analysis #${id} ${record.status === 'PENDING' ? 'queued' : 'running'}… ${Math.round((Date.now() - startedAt) / 1000)}s`);
      }
    } catch (e) {
      setMsg(`Analysis #${id}: could not read its status (${apiError(e)}). It keeps running; refresh the list later.`);
    }
  }

  async function handleAnalyze() {
    let body;
    try {
      body = buildRcaRequest({
        query,
        traceId,
        timeStart,
        timeEnd,
        connectionId,
        modelName,
        serviceName,
        endpoint,
        statusCode,
        measurement,
        filters: additionalFilters,
      }, { nsId, infraId, nodeId });
    } catch (e) {
      setMsg(e.message);
      return;
    }
    setBusy(true);
    setMsg('');
    try {
      const res = await queryRca(body);
      const id = res?.analysis?.id;
      setShowAnalysisForm(false);
      setMsg(`Analysis #${id || '-'} started`);
      if (page === 1) await load();
      else setPage(1);
      if (id) pollAnalysis(id);
    } catch (e) {
      setMsg('Analysis failed: ' + apiError(e));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex justify-between items-center">
          <span className="font-semibold text-sm">LLM Connections</span>
          <button
            type="button"
            onClick={() => setShowConnectionForm((show) => !show)}
            aria-expanded={showConnectionForm}
            aria-controls="llm-connection-form"
            className="text-xs bg-purple-600 text-white px-3 py-1 rounded hover:bg-purple-700"
          >
            {showConnectionForm ? 'Cancel' : '+ Add Connection'}
          </button>
        </div>
        {showConnectionForm && (
          <div id="llm-connection-form">
            <CreateLlmConnectionForm
              makeDefault={!llmConnections.some((connection) => connection.is_default)}
              onCreated={async (connection) => {
                setShowConnectionForm(false);
                await loadConnections();
                setConnectionId(String(connection.id));
                setMsg(`Connection "${connection.name}" added.`);
              }}
            />
          </div>
        )}
        <div className="p-4 overflow-auto">
          {llmConnections.length === 0 ? (
            <p className="text-sm text-gray-400">No connections configured</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                {LLM_COLUMNS.map((column) => (
                  <th key={column.label} className={`px-3 py-2 border-b text-xs text-gray-500 ${column.className || ''}`}>
                    {column.label}
                  </th>
                ))}
              </tr></thead>
              <tbody>
                {llmConnections.map((connection) => (
                  <Fragment key={connection.id}>
                  <tr className="hover:bg-gray-50">
                    <td className="px-3 py-2 border-b font-medium">{connection.name}</td>
                    <td className="px-3 py-2 border-b uppercase text-xs">{connection.provider}</td>
                    <td className="px-3 py-2 border-b text-xs break-all">{connection.base_url || 'OpenAI default'}</td>
                    <td className="px-3 py-2 border-b">{connection.default_model || '-'}</td>
                    <td className="px-3 py-2 border-b">
                      {connection.is_default ? <span className="text-xs px-2 py-0.5 rounded-full bg-blue-100 text-blue-700">Default</span> : '-'}
                    </td>
                    <td className="px-3 py-2 border-b">
                      <span className={`text-xs px-2 py-0.5 rounded-full ${connection.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                        {connection.enabled ? 'Enabled' : 'Disabled'}
                      </span>
                    </td>
                    <td className="px-3 py-2 border-b text-right whitespace-nowrap space-x-3">
                      {!connection.is_default && (
                        <button type="button"
                          disabled={!connection.enabled || !connection.default_model || defaultingId === connection.id}
                          title={!connection.enabled ? 'Enable the connection first'
                            : !connection.default_model ? 'Set a default model first' : ''}
                          onClick={() => handleSetDefault(connection)}
                          className="text-xs text-blue-600 hover:text-blue-800 disabled:text-gray-300 disabled:cursor-not-allowed">
                          {defaultingId === connection.id ? 'Setting…' : 'Set default'}
                        </button>
                      )}
                      <button type="button" aria-expanded={editingId === connection.id}
                        onClick={() => setEditingId((current) => current === connection.id ? null : connection.id)}
                        className="text-xs text-gray-600 hover:text-gray-900">
                        {editingId === connection.id ? 'Close' : 'Edit'}
                      </button>
                      {/* The server refuses to delete the default connection, so don't offer it. */}
                      <button type="button" disabled={connection.is_default || deletingId === connection.id}
                        title={connection.is_default ? 'Set another connection as default first' : ''}
                        onClick={() => handleDeleteConnection(connection)}
                        className="text-xs text-red-500 hover:text-red-700 disabled:text-gray-300 disabled:cursor-not-allowed">
                        {deletingId === connection.id ? 'Deleting…' : 'Delete'}
                      </button>
                    </td>
                  </tr>
                  {editingId === connection.id && (
                    <tr>
                      <td colSpan={LLM_COLUMNS.length} className="border-b bg-slate-50 p-0">
                        <EditLlmConnectionForm
                          connection={connection}
                          onCancel={() => setEditingId(null)}
                          onUpdated={async (updated) => {
                            setEditingId(null);
                            await loadConnections();
                            setMsg(`Connection "${updated?.name || connection.name}" updated.`);
                          }}
                        />
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

      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex flex-wrap items-center gap-3">
          <span className="font-semibold text-sm">Automatic RCA</span>
          <span className="text-xs text-gray-400">
            Schedules repeat a question on an interval; watches analyse HTTP 5xx server errors when they occur.
          </span>
          <button type="button"
            onClick={() => { setMode('schedule'); setShowAnalysisForm(true); document.getElementById('rca-analysis-form')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }}
            className="ml-auto rounded-md border border-purple-600 px-3 py-1.5 text-xs font-semibold text-purple-700 hover:bg-purple-50">
            + New schedule or watch
          </button>
        </div>
        <div className="p-4 overflow-auto">
          {schedules.length === 0 ? (
            <p className="text-sm text-gray-400">
              No schedules yet — use “+ New schedule or watch” and pick “Repeat on a schedule” or “Watch for server errors”.
            </p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                {['Name', 'Every', 'Trigger', 'Enabled', 'Last status', 'Last run', 'Next run', 'Analysis'].map((label) => (
                  <th key={label} className="px-3 py-2 border-b text-xs text-gray-500">{label}</th>
                ))}
                <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Actions</th>
              </tr></thead>
              <tbody>
                {schedules.map((schedule) => {
                  const running = schedule.status === 'RUNNING';
                  const busyRow = running || scheduleBusyId === schedule.id;
                  return (
                    <tr key={schedule.id} className="hover:bg-gray-50">
                      <td className="px-3 py-2 border-b font-medium break-words">{schedule.name}</td>
                      <td className="px-3 py-2 border-b">{schedule.interval_minutes}m</td>
                      <td className="px-3 py-2 border-b text-xs text-gray-600">
                        {schedule.trigger === 'server_error' ? 'Server error' : '-'}
                      </td>
                      <td className="px-3 py-2 border-b">
                        <span className={`text-xs px-2 py-0.5 rounded-full ${schedule.enabled ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'}`}>
                          {schedule.enabled ? 'On' : 'Off'}
                        </span>
                      </td>
                      <td className="px-3 py-2 border-b">
                        {/* A watch that found nothing did its job; it is not a skipped run. */}
                        <SeBadge status={schedule.status === 'SKIPPED' ? 'No server errors' : schedule.status} />
                      </td>
                      <td className="px-3 py-2 border-b text-xs text-gray-500">{fmt(schedule.last_execution)}</td>
                      <td className="px-3 py-2 border-b text-xs text-gray-500">{fmt(schedule.next_execution)}</td>
                      <td className="px-3 py-2 border-b text-xs text-gray-500">
                        {schedule.last_analysis_id ? `#${schedule.last_analysis_id}` : '-'}
                        {schedule.last_error && schedule.status !== 'SKIPPED' && (
                          <p className="mt-1 text-[11px] text-red-500 break-words">{schedule.last_error}</p>
                        )}
                      </td>
                      <td className="px-3 py-2 border-b text-right whitespace-nowrap space-x-3">
                        {/* A running analysis owns the row until it reports back. */}
                        <button type="button" disabled={busyRow}
                          title={running ? 'Wait for the running analysis to finish' : ''}
                          onClick={() => handleScheduleAction(schedule, { enabled: !schedule.enabled })}
                          className="text-xs text-blue-600 hover:text-blue-800 disabled:text-gray-300 disabled:cursor-not-allowed">
                          {schedule.enabled ? 'Disable' : 'Enable'}
                        </button>
                        <button type="button" disabled={busyRow}
                          onClick={() => {
                            if (confirm(`Delete schedule "${schedule.name}"? Past analyses are kept.`)) {
                              handleScheduleAction(schedule, null);
                            }
                          }}
                          className="text-xs text-red-500 hover:text-red-700 disabled:text-gray-300 disabled:cursor-not-allowed">
                          Delete
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>

      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex flex-wrap items-center gap-3">
          <span className="font-semibold text-sm">RCA Analyses</span>
          <div className="ml-auto flex flex-wrap items-end gap-2">
            <button
              type="button"
              onClick={() => setShowAnalysisForm((show) => !show)}
              aria-expanded={showAnalysisForm}
              aria-controls="rca-analysis-form"
              className="text-xs bg-purple-600 text-white px-3 py-1 rounded hover:bg-purple-700"
            >
              {showAnalysisForm ? 'Cancel' : '+ New Analysis'}
            </button>
            <label className="text-[11px] text-gray-500">
              Status
              <select className="block border rounded px-2 py-1 text-xs text-gray-700" value={status}
                onChange={(e) => { setStatus(e.target.value); setPage(1); }}>
                {SE_STATUS.map((s) => <option key={s} value={s}>{s || 'All status'}</option>)}
              </select>
            </label>
            <label className="text-[11px] text-gray-500">
              From
              <input type="datetime-local" value={listFrom}
                onChange={(e) => { setListFrom(e.target.value); setPage(1); }}
                className="block border rounded px-2 py-1 text-xs text-gray-700" />
            </label>
            <label className="text-[11px] text-gray-500">
              To
              <input type="datetime-local" value={listTo}
                onChange={(e) => { setListTo(e.target.value); setPage(1); }}
                className="block border rounded px-2 py-1 text-xs text-gray-700" />
            </label>
            <label className="text-[11px] text-gray-500">
              Page size
              <select value={pageSize}
                onChange={(e) => { setPageSize(Number(e.target.value)); setPage(1); }}
                className="block border rounded px-2 py-1 text-xs text-gray-700">
                {[20, 50, 100].map((value) => <option key={value} value={value}>{value}</option>)}
              </select>
            </label>
            <button type="button" onClick={() => load()} className="h-7 text-xs text-gray-500 hover:text-gray-800 px-2">
              Refresh
            </button>
          </div>
        </div>
        {showAnalysisForm && (
          <form
            id="rca-analysis-form"
            onSubmit={(e) => { e.preventDefault(); handleSubmit(); }}
            className="p-4 border-b bg-gray-50 space-y-4"
          >
            {/* 1. What to create — decides which fields below apply. */}
            <div className="rounded-lg border border-slate-200 bg-white p-3 space-y-3">
              <div className="flex flex-wrap gap-2" role="radiogroup" aria-label="What to create">
                {[
                  ['once', 'Run once', 'Analyse a question now'],
                  ['schedule', 'Repeat on a schedule', 'Re-ask the question every N minutes'],
                  ['watch', 'Watch for server errors', 'Every N minutes, analyse HTTP 5xx server errors if any occurred'],
                ].map(([value, label, hint]) => (
                  <button key={value} type="button" role="radio" aria-checked={mode === value}
                    onClick={() => setMode(value)}
                    className={`flex-1 min-w-[12rem] rounded-md border px-3 py-2 text-left ${mode === value
                      ? 'border-purple-600 bg-purple-50 text-purple-800'
                      : 'border-slate-200 text-slate-700 hover:bg-slate-50'}`}>
                    <span className="block text-sm font-medium">{label}</span>
                    <span className="block text-[11px] text-slate-500">{hint}</span>
                  </button>
                ))}
              </div>
              {mode !== 'once' && (
                <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                  <div className="md:col-span-3">
                    <label htmlFor="rca-schedule-name" className="block text-xs text-gray-600 mb-1">Name</label>
                    <input id="rca-schedule-name" value={scheduleName} onChange={(e) => setScheduleName(e.target.value)} maxLength={100}
                      placeholder={mode === 'watch' ? 'payment 5xx' : 'checkout errors'}
                      className="border rounded px-3 py-1.5 text-sm w-full" />
                  </div>
                  <div>
                    <label htmlFor="rca-schedule-interval" className="block text-xs text-gray-600 mb-1">Every (min)</label>
                    <input id="rca-schedule-interval" type="number" min={5} max={10080} value={scheduleInterval}
                      onChange={(e) => setScheduleInterval(e.target.value)}
                      className="border rounded px-3 py-1.5 text-sm w-full" />
                  </div>
                  <p className="md:col-span-4 text-[11px] text-slate-500">
                    {mode === 'watch'
                      ? 'Each run checks the interval that just ended for HTTP 5xx server errors and runs a root-cause analysis only when some are found; otherwise it is marked "No server errors".'
                      : 'Each run analyses the interval that just ended; a schedule ignores Start and End.'}
                  </p>
                </div>
              )}
            </div>

            {/* 2. The request: question, time and model. */}
            <div className="rounded-lg border border-slate-200 bg-white p-3 space-y-3">
              {mode !== 'watch' && (
                <div>
                  <label htmlFor="rca-query" className="block text-xs font-medium text-gray-700 mb-1">Analysis request</label>
                  <textarea id="rca-query" value={query} onChange={(e) => setQuery(e.target.value)}
                    maxLength={8000}
                    className="border rounded px-3 py-2 text-sm w-full h-20"
                    placeholder="Describe the incident or question to investigate" />
                </div>
              )}
              <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                {mode === 'once' && (
                  <>
                    <div>
                      <label htmlFor="rca-time-start" className="block text-xs text-gray-600 mb-1">Start</label>
                      <input id="rca-time-start" type="datetime-local" value={timeStart} onChange={(e) => setTimeStart(e.target.value)}
                        className="border rounded px-3 py-1.5 text-sm w-full" />
                    </div>
                    <div>
                      <label htmlFor="rca-time-end" className="block text-xs text-gray-600 mb-1">End</label>
                      <input id="rca-time-end" type="datetime-local" value={timeEnd} onChange={(e) => setTimeEnd(e.target.value)}
                        className="border rounded px-3 py-1.5 text-sm w-full" />
                    </div>
                  </>
                )}
                <div>
                  <label htmlFor="rca-connection" className="block text-xs text-gray-600 mb-1">Connection</label>
                  <select
                    id="rca-connection"
                    value={connectionId}
                    disabled={enabledConnections.length === 0}
                    onChange={(e) => {
                      const nextId = e.target.value;
                      const connection = enabledConnections.find((item) => String(item.id) === nextId);
                      setConnectionId(nextId);
                      setConnectionModels(connection?.default_model ? [connection.default_model] : []);
                      setModelName(connection?.default_model || '');
                    }}
                    className="border rounded px-2 py-1.5 text-sm w-full disabled:bg-gray-100"
                  >
                    {enabledConnections.length === 0 && <option value="">No enabled connection</option>}
                    {enabledConnections.map((connection) => (
                      <option key={connection.id} value={connection.id}>
                        {connection.name} ({connection.provider})
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label htmlFor="rca-model" className="block text-xs text-gray-600 mb-1">Model</label>
                  <select id="rca-model" value={modelName} disabled={connectionModels.length === 0}
                    onChange={(e) => setModelName(e.target.value)}
                    className="border rounded px-2 py-1.5 text-sm w-full disabled:bg-gray-100">
                    {connectionModels.length === 0 && <option value="">No model</option>}
                    {connectionModels.map((mn) => <option key={mn} value={mn}>{mn}</option>)}
                  </select>
                </div>
              </div>
              {mode === 'once' && (
                <p className="text-xs text-gray-500">
                  Start and End are optional; leave them empty to analyse the last 30 minutes. Provide both if you set either.
                </p>
              )}
            </div>

            {/* 3. Where to look: the route context plus per-source scope. */}
            <div className="rounded-lg border border-slate-200 bg-white">
              <div className="flex flex-wrap items-center gap-2 border-b border-slate-200 px-3 py-2">
                <span className="text-sm font-medium text-slate-800">Investigation scope</span>
                <span className="rounded-full bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-slate-500">Optional</span>
                <span className="ml-auto flex flex-wrap items-center gap-2">
                  <span className="text-xs text-slate-500">Context</span>
                  {nsId && <span className="text-xs rounded-full bg-slate-100 text-slate-600 px-2 py-0.5">NS {nsId}</span>}
                  {infraId && <span className="text-xs rounded-full bg-slate-100 text-slate-600 px-2 py-0.5">Infra {infraId}</span>}
                  {nodeId && <span className="text-xs rounded-full bg-slate-100 text-slate-600 px-2 py-0.5">Node {nodeId}</span>}
                  {!nsId && !infraId && !nodeId && <span className="text-xs text-gray-400">all namespaces</span>}
                </span>
              </div>
              <div className="space-y-3 p-3">
                <div className="grid grid-cols-1 md:grid-cols-[9rem_1fr] gap-3 items-start">
                  <div className="pt-1">
                    <span className="block text-xs font-medium text-slate-700">Log · Trace</span>
                    <span className="block text-[11px] text-slate-500">applies to both sources</span>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <div>
                      <label htmlFor="rca-service-name" className="block text-xs text-gray-600 mb-1">Service</label>
                      <input id="rca-service-name" value={serviceName} onChange={(e) => setServiceName(e.target.value)}
                        className="border rounded px-3 py-1.5 text-sm w-full" placeholder="checkout-api" />
                    </div>
                  </div>
                </div>
                {mode !== 'watch' && (
                  <div className="grid grid-cols-1 md:grid-cols-[9rem_1fr] gap-3 items-start">
                    <div className="pt-1">
                      <span className="block text-xs font-medium text-slate-700">Trace</span>
                      <span className="block text-[11px] text-slate-500">span route, status, one trace</span>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                      <div>
                        <label htmlFor="rca-endpoint" className="block text-xs text-gray-600 mb-1">Endpoint (route)</label>
                        <input id="rca-endpoint" value={endpoint} onChange={(e) => setEndpoint(e.target.value)}
                          className="border rounded px-3 py-1.5 text-sm w-full" placeholder="POST /checkout" />
                      </div>
                      <div>
                        <label htmlFor="rca-status-code" className="block text-xs text-gray-600 mb-1">HTTP status code</label>
                        <input id="rca-status-code" value={statusCode} onChange={(e) => setStatusCode(e.target.value)}
                          className="border rounded px-3 py-1.5 text-sm w-full" placeholder="500 or 5xx" />
                      </div>
                      {mode === 'once' && (
                        <div>
                          <label htmlFor="rca-trace-id" className="block text-xs text-gray-600 mb-1">Trace ID</label>
                          <input id="rca-trace-id" value={traceId} onChange={(e) => setTraceId(e.target.value)}
                            className="border rounded px-3 py-1.5 text-sm w-full" />
                        </div>
                      )}
                    </div>
                  </div>
                )}
                <div className="grid grid-cols-1 md:grid-cols-[9rem_1fr] gap-3 items-start">
                  <div className="pt-1">
                    <span className="block text-xs font-medium text-slate-700">Metric</span>
                    <span className="block text-[11px] text-slate-500">Telegraf measurement</span>
                  </div>
                  <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                    <div>
                      <label htmlFor="rca-measurement" className="block text-xs text-gray-600 mb-1">Measurement</label>
                      <input id="rca-measurement" value={measurement} onChange={(e) => setMeasurement(e.target.value)}
                        className="border rounded px-3 py-1.5 text-sm w-full" placeholder="cpu" />
                    </div>
                  </div>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-[9rem_1fr] gap-3 items-start">
                  <div className="pt-1">
                    <span className="block text-xs font-medium text-slate-700">Filters</span>
                    <span className="block text-[11px] text-slate-500">custom labels or tags, every source</span>
                  </div>
                  <div>
                    <div className="space-y-2">
                      {additionalFilters.map((filter, index) => (
                        <div key={index} className="flex gap-2">
                          <input aria-label={`Additional filter ${index + 1} key`} value={filter.key}
                            onChange={(e) => setAdditionalFilters((current) => current.map((item, itemIndex) => (
                              itemIndex === index ? { ...item, key: e.target.value } : item
                            )))}
                            className="border rounded px-3 py-1.5 text-sm flex-1 min-w-0" placeholder="Key, e.g. region" />
                          <input aria-label={`Additional filter ${index + 1} value`} value={filter.value}
                            onChange={(e) => setAdditionalFilters((current) => current.map((item, itemIndex) => (
                              itemIndex === index ? { ...item, value: e.target.value } : item
                            )))}
                            className="border rounded px-3 py-1.5 text-sm flex-1 min-w-0" placeholder="Value" />
                          <button type="button" aria-label={`Remove additional filter ${index + 1}`}
                            onClick={() => setAdditionalFilters((current) => (
                              current.length === 1
                                ? [{ key: '', value: '' }]
                                : current.filter((_, itemIndex) => itemIndex !== index)
                            ))}
                            className="px-2 text-gray-400 hover:text-red-600">×</button>
                        </div>
                      ))}
                    </div>
                    <button type="button"
                      onClick={() => setAdditionalFilters((current) => [...current, { key: '', value: '' }])}
                      className="mt-2 text-xs text-blue-600 hover:text-blue-800">+ Add filter</button>
                  </div>
                </div>
              </div>
            </div>

            {/* 4. One action, named after what it creates. */}
            <div className="flex flex-col gap-3 border-t border-slate-200 pt-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                {enabledConnections.length === 0 ? (
                  <p className="text-xs font-medium text-amber-700">Add an enabled LLM connection before starting an analysis.</p>
                ) : mode === 'once' ? (
                  <p className="text-xs text-slate-500">The analysis runs on the server for a few minutes; the result appears in the list below (you may leave this page).</p>
                ) : (
                  <p className="text-xs text-slate-500">Saved schedules and watches appear in “Automatic RCA” above; each run's result appears in the list below.</p>
                )}
              </div>
              <button type="submit" aria-busy={busy}
                disabled={busy || !connectionId || !modelName || (mode !== 'once' && !scheduleName.trim())}
                className="inline-flex min-h-10 items-center justify-center gap-2 rounded-md bg-purple-600 px-5 py-2 text-sm font-semibold text-white shadow-sm hover:bg-purple-700 focus:outline-none focus:ring-2 focus:ring-purple-500 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50">
                {busy && (
                  <svg aria-hidden="true" viewBox="0 0 24 24" className="h-4 w-4 animate-spin">
                    <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeOpacity="0.3" strokeWidth="3" />
                    <path d="M21 12a9 9 0 0 0-9-9" fill="none" stroke="currentColor" strokeLinecap="round" strokeWidth="3" />
                  </svg>
                )}
                {busy ? 'Working…' : mode === 'once' ? 'Run RCA analysis' : mode === 'schedule' ? 'Save schedule' : 'Save watch'}
              </button>
            </div>
          </form>
        )}
        {msg && <p className="text-xs text-gray-500 px-4 pt-3">{msg}</p>}
        <div className="p-4 overflow-auto">
          {loading ? <p className="text-sm text-gray-400">Loading...</p> : records.length === 0 ? (
            <p className="text-sm text-gray-400">No analysis records</p>
          ) : (
            <table className="w-full min-w-[820px] table-fixed text-sm">
              <thead><tr className="bg-gray-50 text-left">
                {RCA_COLUMNS.map((column, index) => (
                  <th key={index} className={`px-3 py-2 border-b text-xs text-gray-500 ${column.width}`}>
                    {column.label}
                  </th>
                ))}
              </tr></thead>
              <tbody>
                {records.map((record) => {
                  const view = getRcaRecordView(record);
                  return (
                    <Fragment key={record.id}>
                      <tr onClick={() => openDetail(record)}
                        className={`cursor-pointer align-top hover:bg-slate-50 ${selected === record.id ? 'bg-slate-100' : ''}`}>
                        <td className="px-3 py-3 border-b text-gray-400">{selected === record.id ? '▼' : '▶'}</td>
                        <td className="px-3 py-3 border-b">
                          <div className="flex flex-col items-start gap-1">
                            <SeBadge status={record.status} />
                            <RiskBadge risk={view.riskLevel} />
                          </div>
                        </td>
                        <td className="px-3 py-3 border-b">
                          <p className="font-medium text-gray-800 break-words">{view.service || 'Unscoped service'}</p>
                          <p className="mt-0.5 text-xs text-gray-500 break-words">{view.endpoint || 'No endpoint'}</p>
                          <p className="mt-1 font-mono text-[11px] text-gray-400 truncate"
                            title={view.traceId || ''}>{view.traceId || 'No trace ID'}</p>
                        </td>
                        <td className="px-3 py-3 border-b">
                          <p className="font-medium text-gray-800 break-words">
                            {view.noUsableEvidence ? 'No evidence-based conclusion' : view.cause || 'No conclusion yet'}
                          </p>
                          {view.summary && view.summary !== view.cause && (
                            <p className="mt-1 text-xs leading-5 text-gray-500 max-h-10 overflow-hidden">{view.summary}</p>
                          )}
                        </td>
                        <td className="px-3 py-3 border-b">
                          <p className="font-semibold text-gray-800">{view.confidenceLabel}</p>
                          <p className="mt-1 text-[11px] text-gray-500">{view.conclusionStrength || 'Not assessed'}</p>
                        </td>
                        <td className="px-3 py-3 border-b text-xs text-gray-500">
                          <p>{fmt(record.created_at)}</p>
                          <p className="mt-1">{formatRcaDuration(record.created_at, record.updated_at, record.status)}</p>
                          <p className="mt-1 text-[11px] text-gray-400">#{record.id}</p>
                        </td>
                      </tr>
                      {selected === record.id && (
                        <tr>
                          <td colSpan={RCA_COLUMNS.length} className="border-b bg-slate-50 p-4">
                            <RcaDetail record={record} />
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
        <div className="border-t px-4 py-3 flex flex-wrap items-center justify-between gap-3 text-xs text-gray-500">
          <span>
            {total === 0 ? '0 analyses' : `${((page - 1) * pageSize) + 1}–${Math.min(page * pageSize, total)} of ${total}`}
          </span>
          <div className="flex items-center gap-2">
            <button type="button" disabled={page <= 1 || loading} onClick={() => setPage((current) => current - 1)}
              className="border rounded px-3 py-1 text-gray-600 hover:bg-gray-50 disabled:opacity-40">Previous</button>
            <span>Page {page} of {totalPages}</span>
            <button type="button" disabled={page >= totalPages || loading} onClick={() => setPage((current) => current + 1)}
              className="border rounded px-3 py-1 text-gray-600 hover:bg-gray-50 disabled:opacity-40">Next</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function RcaDetail({ record }) {
  const view = getRcaRecordView(record);
  const scope = view.request.scope || {};
  const timeRange = scope.time_range || {};
  const attributes = scope.attributes || {};
  const filters = view.request.filters || {};

  return (
    <div className="space-y-4">
      {view.errors.length > 0 && (
        <div className="rounded border border-red-200 bg-red-50 px-3 py-2 text-xs text-red-700">
          {view.errors.join(' · ')}
        </div>
      )}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
        <section className="lg:col-span-2 rounded border border-slate-200 bg-white p-4">
          <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">Probable cause</h4>
          <p className="mt-2 text-base font-semibold text-slate-900 whitespace-pre-wrap">
            {view.noUsableEvidence ? 'No evidence-based conclusion' : view.cause || 'No conclusion is available.'}
          </p>
          {view.summary && view.summary !== view.cause && (
            <p className="mt-3 text-sm leading-6 text-slate-600 whitespace-pre-wrap">{view.summary}</p>
          )}
        </section>
        <section className="rounded border border-slate-200 bg-white p-4">
          <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">Assessment</h4>
          <div className="mt-3 flex flex-wrap gap-2">
            <SeBadge status={record.status} />
            <RiskBadge risk={view.riskLevel} />
          </div>
          <dl className="mt-3 space-y-2 text-xs">
            <div className="flex justify-between gap-4">
              <dt className="text-gray-500">Confidence</dt>
              <dd className="font-semibold text-gray-800">{view.confidenceLabel}</dd>
            </div>
            <div className="flex justify-between gap-4">
              <dt className="text-gray-500">Conclusion</dt>
              <dd className="font-semibold text-gray-800">{view.conclusionStrength || 'Not assessed'}</dd>
            </div>
          </dl>
          {Object.keys(view.sources).length > 0 && (
            <div className="mt-3 border-t border-slate-100 pt-3">
              <p className="text-[11px] font-semibold uppercase tracking-wide text-slate-400">Evidence collection</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {['trace', 'log', 'metric'].map((source) => view.sources[source] && (
                  <span key={source} className="inline-flex items-center gap-1.5 text-xs capitalize text-slate-500">
                    {source}
                    <SeBadge status={view.sources[source]} />
                  </span>
                ))}
              </div>
            </div>
          )}
        </section>
      </div>

      <section className="rounded border border-slate-200 bg-white p-4">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">Evidence</h4>
        <div className="mt-3 space-y-2">
          {view.evidence.map((item, index) => (
            <div key={item.evidence_id || index} className="rounded bg-slate-50 px-3 py-2 text-xs">
              <div className="flex flex-wrap gap-2 text-[11px] uppercase tracking-wide text-slate-500">
                <span>{item.source || 'evidence'}</span>
                {item.evidence_id && <span>{item.evidence_id}</span>}
                {item.signal && <span>{item.signal}</span>}
              </div>
              <p className="mt-1 text-slate-700">{item.observation || String(item)}</p>
            </div>
          ))}
          {view.evidence.length === 0 && (
            <p className="text-xs text-gray-400">No grounded evidence items are available.</p>
          )}
        </div>
      </section>

      <section className="rounded border border-slate-200 bg-white p-4">
        <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">Hypotheses</h4>
        {view.hypotheses.length === 0 ? (
          <p className="mt-2 text-xs text-gray-400">No ranked hypotheses are available.</p>
        ) : (
          <ol className="mt-3 space-y-2">
            {view.hypotheses.map((hypothesis, index) => (
              <li key={index} className="flex gap-3 text-sm text-slate-700">
                <span className="font-mono text-xs text-slate-400">{index + 1}</span>
                <span className="flex-1">{hypothesis.cause || String(hypothesis)}</span>
                {typeof hypothesis.confidence === 'number' && (
                  <span className="text-xs font-semibold text-slate-500">
                    {Math.round(hypothesis.confidence * 100)}%
                  </span>
                )}
              </li>
            ))}
          </ol>
        )}
      </section>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-3">
        <RcaTextList title="Mitigation" items={view.mitigation} empty="No mitigation was proposed." />
        <RcaTextList title="Next checks" items={view.nextChecks} empty="No follow-up checks were proposed." />
        <RcaTextList title="Limitations" items={view.limitations} empty="No limitations were reported." />
      </div>

      <details className="rounded border border-slate-200 bg-white">
        <summary className="cursor-pointer px-4 py-3 text-xs font-semibold text-slate-600">Request scope</summary>
        <div className="border-t p-4 text-xs">
          <dl className="grid grid-cols-1 md:grid-cols-2 gap-3">
            <div><dt className="text-gray-400">Trace ID</dt><dd className="mt-1 font-mono break-all">{view.traceId || '-'}</dd></div>
            <div><dt className="text-gray-400">Service</dt><dd className="mt-1">{view.service || '-'}</dd></div>
            <div><dt className="text-gray-400">Endpoint</dt><dd className="mt-1">{view.endpoint || '-'}</dd></div>
            <div><dt className="text-gray-400">HTTP status</dt><dd className="mt-1">{scope.status_code || filters.status_code || '-'}</dd></div>
            <div><dt className="text-gray-400">Start</dt><dd className="mt-1">{fmt(timeRange.start)}</dd></div>
            <div><dt className="text-gray-400">End</dt><dd className="mt-1">{fmt(timeRange.end)}</dd></div>
          </dl>
          {(Object.keys(attributes).length > 0 || Object.keys(filters).length > 0) && (
            <pre className="mt-3 max-h-48 overflow-auto rounded bg-slate-50 p-3 text-[11px]">
              {JSON.stringify({ attributes, filters }, null, 2)}
            </pre>
          )}
        </div>
      </details>
    </div>
  );
}

function RcaTextList({ title, items, empty }) {
  return (
    <section className="rounded border border-slate-200 bg-white p-4">
      <h4 className="text-xs font-semibold uppercase tracking-wide text-slate-500">{title}</h4>
      {items.length === 0 ? (
        <p className="mt-2 text-xs text-gray-400">{empty}</p>
      ) : (
        <ul className="mt-3 space-y-2 text-xs leading-5 text-slate-700">
          {items.map((item, index) => <li key={index} className="flex gap-2"><span>•</span><span>{item}</span></li>)}
        </ul>
      )}
    </section>
  );
}

function RiskBadge({ risk }) {
  const value = (risk || '').toUpperCase();
  const color = value === 'CRITICAL' ? 'bg-red-100 text-red-700'
    : value === 'HIGH' ? 'bg-orange-100 text-orange-700'
    : value === 'MEDIUM' ? 'bg-yellow-100 text-yellow-700'
    : value === 'LOW' ? 'bg-emerald-100 text-emerald-700'
    : 'bg-gray-100 text-gray-500';
  return <span className={`text-[11px] px-2 py-0.5 rounded-full ${color}`}>{value || 'Not rated'}</span>;
}

// Shared field row for both the create and the edit form. `idPrefix` keeps the label
// targets unique when an edit row is open while the create form is expanded.
function LlmConnectionFields({ idPrefix, value, onChange, apiKeyPlaceholder }) {
  const set = (patch) => onChange({ ...value, ...patch });
  return (
    <div className="grid grid-cols-1 md:grid-cols-5 gap-3">
      <div>
        <label htmlFor={`${idPrefix}-name`} className="block text-xs text-gray-600 mb-1">Name</label>
        <input id={`${idPrefix}-name`} required maxLength={100} value={value.name}
          onChange={(e) => set({ name: e.target.value })}
          className="w-full border rounded px-3 py-1.5 text-sm" placeholder="Production LLM" />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-provider`} className="block text-xs text-gray-600 mb-1">Provider</label>
        <select id={`${idPrefix}-provider`} value={value.provider} onChange={(e) => set({ provider: e.target.value })}
          className="w-full border rounded px-3 py-1.5 text-sm">
          <option value="openai">OpenAI</option>
          <option value="ollama">Ollama</option>
        </select>
      </div>
      <div>
        <label htmlFor={`${idPrefix}-base-url`} className="block text-xs text-gray-600 mb-1">Base URL</label>
        <input id={`${idPrefix}-base-url`} type="url" required={value.provider === 'ollama'} value={value.baseUrl}
          onChange={(e) => set({ baseUrl: e.target.value })}
          className="w-full border rounded px-3 py-1.5 text-sm"
          placeholder={value.provider === 'ollama' ? 'http://ollama:11434' : 'OpenAI default'} />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-api-key`} className="block text-xs text-gray-600 mb-1">API Key</label>
        <input id={`${idPrefix}-api-key`} type="password" autoComplete="new-password" value={value.apiKey}
          onChange={(e) => set({ apiKey: e.target.value })}
          className="w-full border rounded px-3 py-1.5 text-sm"
          placeholder={apiKeyPlaceholder ?? (value.provider === 'openai' ? 'Required for OpenAI' : 'Optional')} />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-default-model`} className="block text-xs text-gray-600 mb-1">Default Model</label>
        <input id={`${idPrefix}-default-model`} required maxLength={255} value={value.defaultModel}
          onChange={(e) => set({ defaultModel: e.target.value })}
          className="w-full border rounded px-3 py-1.5 text-sm"
          placeholder={value.provider === 'ollama' ? 'llama3.1' : 'gpt-4o-mini'} />
      </div>
      <div>
        <label htmlFor={`${idPrefix}-context-length`} className="block text-xs text-gray-600 mb-1">Context Length</label>
        <input id={`${idPrefix}-context-length`} type="number" min={1024} max={10000000} step={1024}
          value={value.contextLength}
          onChange={(e) => set({ contextLength: e.target.value })}
          className="w-full border rounded px-3 py-1.5 text-sm"
          placeholder="Leave empty to use the server default" />
        <p className="mt-1 text-xs text-gray-500">
          Input tokens this endpoint actually serves. Ollama sizes it from host VRAM, so the same
          model differs per server. If left empty and the window cannot be detected, analysis
          budgets assume a large window and the server may silently drop the oldest messages.
        </p>
      </div>
    </div>
  );
}

// '' -> null so PATCH clears the column; the API rejects anything below 1024.
function parseContextLength(raw) {
  const trimmed = String(raw ?? '').trim();
  if (!trimmed) return null;
  const parsed = Number(trimmed);
  return Number.isFinite(parsed) ? Math.trunc(parsed) : null;
}

function CreateLlmConnectionForm({ makeDefault, onCreated }) {
  const [value, setValue] = useState({ name: '', provider: 'openai', baseUrl: '', apiKey: '', defaultModel: '', contextLength: '' });
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    setErr('');
    const body = {
      name: value.name.trim(),
      provider: value.provider,
      base_url: value.baseUrl.trim() || null,
      api_key: value.apiKey.trim() || null,
      default_model: value.defaultModel.trim(),
      context_length: parseContextLength(value.contextLength),
      enabled: true,
      is_default: makeDefault,
    };
    try {
      onCreated(await createLlmConnection(body));
    } catch (e2) {
      setErr(apiError(e2));
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="p-4 border-b bg-gray-50 space-y-3">
      <LlmConnectionFields idPrefix="llm-create" value={value} onChange={setValue} />
      {makeDefault && <p className="text-xs text-gray-500">The first available connection becomes the default.</p>}
      {err && <p className="text-xs text-red-500">{err}</p>}
      <button type="submit" disabled={busy} className="bg-purple-600 text-white px-4 py-1.5 rounded text-sm hover:bg-purple-700 disabled:opacity-50">
        {busy ? 'Adding...' : 'Add Connection'}
      </button>
    </form>
  );
}

function EditLlmConnectionForm({ connection, onUpdated, onCancel }) {
  const [value, setValue] = useState({
    name: connection.name || '',
    provider: connection.provider || 'openai',
    baseUrl: connection.base_url || '',
    apiKey: '',
    defaultModel: connection.default_model || '',
    contextLength: connection.context_length == null ? '' : String(connection.context_length),
  });
  const [enabled, setEnabled] = useState(Boolean(connection.enabled));
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState('');

  const nextBaseUrl = value.baseUrl.trim() || null;
  // Moving the endpoint discards the stored key server-side, and the official OpenAI
  // endpoint then rejects the update outright unless a new key comes with it.
  const endpointChanged = value.provider !== connection.provider || nextBaseUrl !== (connection.base_url || null);

  async function submit(e) {
    e.preventDefault();
    // PATCH takes only the changed fields; an empty body is rejected as "at least one
    // connection field is required", and an unsent api_key keeps the stored one.
    const body = {};
    if (value.name.trim() !== connection.name) body.name = value.name.trim();
    if (value.provider !== connection.provider) body.provider = value.provider;
    if (nextBaseUrl !== (connection.base_url || null)) body.base_url = nextBaseUrl;
    if (value.apiKey.trim()) body.api_key = value.apiKey.trim();
    if (value.defaultModel.trim() !== connection.default_model) body.default_model = value.defaultModel.trim();
    const nextContextLength = parseContextLength(value.contextLength);
    if (nextContextLength !== (connection.context_length ?? null)) body.context_length = nextContextLength;
    if (enabled !== Boolean(connection.enabled)) body.enabled = enabled;
    if (Object.keys(body).length === 0) { setErr('Nothing changed.'); return; }

    setBusy(true);
    setErr('');
    try {
      onUpdated(await updateLlmConnection(connection.id, body));
    } catch (e2) {
      setErr(apiError(e2));
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="p-4 space-y-3">
      <LlmConnectionFields idPrefix={`llm-edit-${connection.id}`} value={value} onChange={setValue}
        apiKeyPlaceholder={endpointChanged ? 'Re-enter after an endpoint change' : 'Leave blank to keep'} />
      <label className="flex items-center gap-2 text-xs text-gray-600">
        <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
        Enabled
      </label>
      {endpointChanged && (
        <p className="text-xs text-amber-700">
          Changing the provider or base URL clears the stored API key — enter it again to keep the connection usable.
        </p>
      )}
      {connection.is_default && !enabled && (
        <p className="text-xs text-amber-700">Disabling the default connection leaves new analyses without one.</p>
      )}
      {err && <p className="text-xs text-red-500">{err}</p>}
      <div className="flex gap-2">
        <button type="submit" disabled={busy} className="bg-purple-600 text-white px-4 py-1.5 rounded text-sm hover:bg-purple-700 disabled:opacity-50">
          {busy ? 'Saving...' : 'Save'}
        </button>
        <button type="button" onClick={onCancel} className="border px-4 py-1.5 rounded text-sm text-gray-600 hover:bg-gray-50">
          Cancel
        </button>
      </div>
    </form>
  );
}

function SeBadge({ status }) {
  const s = (status || '').toUpperCase();
  const c = s === 'SUCCEEDED' || s === 'OK' ? 'bg-green-100 text-green-700'
    : s === 'RUNNING' ? 'bg-blue-100 text-blue-700'
    : s === 'PENDING' ? 'bg-yellow-100 text-yellow-700'
    : s === 'FAILED' ? 'bg-red-100 text-red-700'
    : s === 'PARTIAL' ? 'bg-orange-100 text-orange-700' : 'bg-gray-100 text-gray-500';
  return <span className={`text-xs px-2 py-0.5 rounded-full ${c}`}>{status || '-'}</span>;
}

function fmt(t) {
  if (!t) return '-';
  return formatLocalTime(t) || '-';
}
