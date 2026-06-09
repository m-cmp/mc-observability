import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { searchTraces, getTrace, getTraceServices } from '../api/trace';

const RANGE_OPTIONS = [
  { value: 1, label: '1H' },
  { value: 6, label: '6H' },
  { value: 12, label: '12H' },
  { value: 24, label: '24H' },
];

const SCOPES = [
  { key: 'framework', label: 'Framework', desc: 'o11y 플랫폼 자체 트레이스 (manager / insight)' },
  { key: 'vm', label: 'VM', desc: '대상 VM 애플리케이션 트레이스 (Beyla / OTel)' },
];

export default function TraceViewer() {
  const { infraId } = useParams();

  const [scope, setScope] = useState('framework');
  const [services, setServices] = useState([]);
  const [service, setService] = useState('');
  const [keyword, setKeyword] = useState('');
  const [rangeHours, setRangeHours] = useState(1);
  const [traces, setTraces] = useState(null);
  const [loading, setLoading] = useState(false);

  // expanded trace detail
  const [selectedTraceId, setSelectedTraceId] = useState('');
  const [spans, setSpans] = useState(null);
  const [spanLoading, setSpanLoading] = useState(false);

  // Load service dropdown whenever scope changes
  useEffect(() => {
    setService('');
    getTraceServices(scope).then(setServices).catch(() => setServices([]));
  }, [scope]);

  const search = useCallback(async () => {
    setLoading(true);
    setSelectedTraceId('');
    setSpans(null);
    try {
      const result = await searchTraces({ scope, service, keyword, rangeHours });
      setTraces(Array.isArray(result) ? result : []);
    } catch (e) {
      console.error('Trace query failed', e);
      setTraces([]);
    }
    setLoading(false);
  }, [scope, service, keyword, rangeHours]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') search();
  };

  async function selectTrace(traceId) {
    if (selectedTraceId === traceId) {
      setSelectedTraceId('');
      setSpans(null);
      return;
    }
    setSelectedTraceId(traceId);
    setSpanLoading(true);
    setSpans(null);
    try {
      const detail = await getTrace(traceId);
      setSpans(detail?.spans || []);
    } catch (e) {
      console.error('Trace detail failed', e);
      setSpans([]);
    }
    setSpanLoading(false);
  }

  return (
    <div className="space-y-4">
      {/* Scope tabs */}
      <div className="flex gap-1 bg-white rounded-lg shadow px-2 py-1 items-center">
        {SCOPES.map((s) => (
          <button key={s.key} onClick={() => setScope(s.key)} title={s.desc}
            className={`px-4 py-2 text-sm rounded ${scope === s.key ? 'bg-teal-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}>
            {s.label}
          </button>
        ))}
        <span className="text-xs text-gray-400 ml-2">{SCOPES.find((s) => s.key === scope)?.desc}</span>
      </div>

      {/* Control card */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Trace Manage</div>
        <div className="p-4">
          <div className="grid grid-cols-4 gap-4 mb-2">
            {/* Service dropdown */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Service</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={service} onChange={(e) => setService(e.target.value)}>
                <option value="">All services ({services.length})</option>
                {services.map((s) => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            {/* Keyword */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Keyword</label>
              <input type="text" placeholder="span / service keyword..." value={keyword} onChange={(e) => setKeyword(e.target.value)} onKeyDown={handleKeyDown}
                className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" />
            </div>
            {/* Range */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Range</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={rangeHours} onChange={(e) => setRangeHours(+e.target.value)}>
                {RANGE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
            </div>
            {/* Search */}
            <div className="flex items-end">
              <button onClick={search} disabled={loading}
                className="px-4 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm">
                {loading ? 'Searching...' : 'Search'}
              </button>
            </div>
          </div>
          <p className="text-xs text-gray-400">
            {scope === 'vm'
              ? 'VM 트레이스는 대상 노드에 trace agent(Beyla / OTel)가 설치되어 있어야 수집됩니다.'
              : 'Framework 트레이스는 o11y 매니저/인사이트가 OTel로 자기계측한 데이터입니다.'}
          </p>
        </div>
      </div>

      {/* Trace list */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">List of Trace</div>
        <div className="p-4 overflow-auto">
          {traces === null ? (
            <p className="text-sm text-gray-400">Click Search to query traces</p>
          ) : traces.length === 0 ? (
            <p className="text-sm text-gray-400">No traces found</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                <th className="px-3 py-2 border-b text-xs text-gray-500">Start Time</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Root Service</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Root Name</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Duration</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Trace ID</th>
              </tr></thead>
              <tbody>
                {traces.map((t) => (
                  <tr key={t.traceId} onClick={() => selectTrace(t.traceId)}
                    className={`cursor-pointer hover:bg-blue-50 ${selectedTraceId === t.traceId ? 'bg-blue-100' : ''}`}>
                    <td className="px-3 py-2 border-b whitespace-nowrap">{fmtTime(t.startTimeMs)}</td>
                    <td className="px-3 py-2 border-b font-medium">{t.rootService || '-'}</td>
                    <td className="px-3 py-2 border-b">{t.rootName || '-'}</td>
                    <td className="px-3 py-2 border-b text-right">{fmtDuration(t.durationMs)}</td>
                    <td className="px-3 py-2 border-b font-mono text-xs text-gray-500">{shortId(t.traceId)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Span call sequence */}
      {selectedTraceId && (
        <div className="bg-white rounded-lg shadow">
          <div className="px-4 py-3 border-b font-semibold text-sm">
            Call Sequence — <span className="font-mono text-xs text-gray-500">{selectedTraceId}</span>
          </div>
          <div className="p-4 overflow-auto">
            {spanLoading ? (
              <p className="text-sm text-gray-400 animate-pulse">Loading spans...</p>
            ) : !spans || spans.length === 0 ? (
              <p className="text-sm text-gray-400">No spans found</p>
            ) : (
              <CallTree spans={spans} />
            )}
          </div>
        </div>
      )}
    </div>
  );
}

/** Renders spans as a parent→child call tree (DFS by start time) — the API call sequence. */
function CallTree({ spans }) {
  const ordered = buildCallTree(spans);
  const t0 = spans.reduce((min, s) => (s.startTimeMs < min ? s.startTimeMs : min), spans[0].startTimeMs);
  const totalDur = Math.max(1, ...spans.map((s) => (s.startTimeMs - t0) + (s.durationMs || 0)));
  return (
    <table className="w-full text-sm">
      <thead><tr className="bg-gray-50 text-left">
        <th className="px-3 py-2 border-b text-xs text-gray-500 w-8">#</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500">Span (call order)</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500">Service</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500">Kind</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Offset</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Duration</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500 w-1/3">Timeline</th>
      </tr></thead>
      <tbody>
        {ordered.map((s, i) => {
          const offset = s.startTimeMs - t0;
          const leftPct = Math.min(100, (offset / totalDur) * 100);
          const widthPct = Math.max(1, Math.min(100 - leftPct, ((s.durationMs || 0) / totalDur) * 100));
          return (
            <tr key={s.spanId || i} className="hover:bg-gray-50">
              <td className="px-3 py-2 border-b text-xs text-gray-400">{i + 1}</td>
              <td className="px-3 py-2 border-b font-medium">
                <span style={{ paddingLeft: `${s.depth * 16}px` }} className="inline-flex items-center gap-1">
                  {s.depth > 0 && <span className="text-gray-300">└</span>}
                  {s.name || '-'}
                </span>
              </td>
              <td className="px-3 py-2 border-b text-gray-600">{s.service || '-'}</td>
              <td className="px-3 py-2 border-b text-xs text-gray-500">{fmtKind(s.kind)}</td>
              <td className="px-3 py-2 border-b text-right text-xs">{offset} ms</td>
              <td className="px-3 py-2 border-b text-right text-xs">{fmtDuration(s.durationMs)}</td>
              <td className="px-3 py-2 border-b">
                <div className="relative h-3 bg-gray-100 rounded">
                  <div className={`absolute h-3 rounded ${kindColor(s.kind)}`} style={{ left: `${leftPct}%`, width: `${widthPct}%` }} />
                </div>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
}

// Build a parent→child tree and flatten it depth-first ordered by start time.
function buildCallTree(spans) {
  const byId = {};
  spans.forEach((s) => { byId[s.spanId] = { ...s, children: [] }; });
  const roots = [];
  spans.forEach((s) => {
    const node = byId[s.spanId];
    if (s.parentSpanId && byId[s.parentSpanId]) byId[s.parentSpanId].children.push(node);
    else roots.push(node);
  });
  const sortRec = (n) => { n.children.sort((a, b) => a.startTimeMs - b.startTimeMs); n.children.forEach(sortRec); };
  roots.sort((a, b) => a.startTimeMs - b.startTimeMs);
  roots.forEach(sortRec);
  const out = [];
  const walk = (n, depth) => { out.push({ ...n, depth }); n.children.forEach((c) => walk(c, depth + 1)); };
  roots.forEach((r) => walk(r, 0));
  return out;
}

function fmtTime(ms) {
  if (!ms) return '-';
  try { return new Date(ms).toLocaleString(); } catch { return String(ms); }
}

function fmtDuration(ms) {
  if (ms == null) return '-';
  if (ms < 1) return `${ms.toFixed(2)} ms`;
  if (ms < 1000) return `${Math.round(ms)} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

function shortId(id) {
  if (!id) return '-';
  return id.length > 16 ? `${id.slice(0, 16)}…` : id;
}

function fmtKind(kind) {
  if (!kind) return '-';
  return String(kind).replace('SPAN_KIND_', '');
}

function kindColor(kind) {
  const k = (kind || '').replace('SPAN_KIND_', '');
  if (k === 'SERVER') return 'bg-emerald-500';
  if (k === 'CLIENT') return 'bg-indigo-500';
  if (k === 'PRODUCER' || k === 'CONSUMER') return 'bg-amber-500';
  return 'bg-blue-500';
}
