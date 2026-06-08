import { useState, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { searchTraces, getTrace } from '../api/trace';

const RANGE_OPTIONS = [
  { value: 1, label: '1H' },
  { value: 6, label: '6H' },
  { value: 12, label: '12H' },
  { value: 24, label: '24H' },
];

export default function TraceViewer() {
  const { infraId } = useParams();

  const [service, setService] = useState('');
  const [keyword, setKeyword] = useState('');
  const [rangeHours, setRangeHours] = useState(1);
  const [traces, setTraces] = useState(null);
  const [loading, setLoading] = useState(false);

  // expanded trace detail
  const [selectedTraceId, setSelectedTraceId] = useState('');
  const [spans, setSpans] = useState(null);
  const [spanLoading, setSpanLoading] = useState(false);

  const search = useCallback(async () => {
    setLoading(true);
    setSelectedTraceId('');
    setSpans(null);
    try {
      const result = await searchTraces({ service, keyword, rangeHours });
      setTraces(Array.isArray(result) ? result : []);
    } catch (e) {
      console.error('Trace query failed', e);
      setTraces([]);
    }
    setLoading(false);
  }, [service, keyword, rangeHours]);

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
      {/* Control card */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Trace Manage</div>
        <div className="p-4">
          <div className="grid grid-cols-4 gap-4 mb-2">
            {/* Workload context */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Workload</label>
              <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={infraId || '-'} readOnly />
            </div>
            {/* Service */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Service</label>
              <input type="text" placeholder="service.name (optional)" value={service} onChange={(e) => setService(e.target.value)} onKeyDown={handleKeyDown}
                className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" />
            </div>
            {/* Keyword */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Keyword</label>
              <input type="text" placeholder="span / service keyword..." value={keyword} onChange={(e) => setKeyword(e.target.value)} onKeyDown={handleKeyDown}
                className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" />
            </div>
            {/* Range + Search */}
            <div className="flex items-end gap-2">
              <div className="flex-1">
                <label className="block text-xs font-medium text-gray-600 mb-1">Range</label>
                <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={rangeHours} onChange={(e) => setRangeHours(+e.target.value)}>
                  {RANGE_OPTIONS.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
                </select>
              </div>
              <button onClick={search} disabled={loading}
                className="px-4 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm">
                {loading ? 'Searching...' : 'Search'}
              </button>
            </div>
          </div>
          <p className="text-xs text-gray-400">Traces are collected via the trace agent (Beyla / OTel) and stored in Tempo.</p>
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

      {/* Span detail */}
      {selectedTraceId && (
        <div className="bg-white rounded-lg shadow">
          <div className="px-4 py-3 border-b font-semibold text-sm">
            Trace Detail — <span className="font-mono text-xs text-gray-500">{selectedTraceId}</span>
          </div>
          <div className="p-4 overflow-auto">
            {spanLoading ? (
              <p className="text-sm text-gray-400 animate-pulse">Loading spans...</p>
            ) : !spans || spans.length === 0 ? (
              <p className="text-sm text-gray-400">No spans found</p>
            ) : (
              <SpanTable spans={spans} />
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function SpanTable({ spans }) {
  const t0 = spans.reduce((min, s) => (s.startTimeMs < min ? s.startTimeMs : min), spans[0].startTimeMs);
  const totalDur = spans.reduce((max, s) => Math.max(max, (s.startTimeMs - t0) + (s.durationMs || 0)), 1);
  return (
    <table className="w-full text-sm">
      <thead><tr className="bg-gray-50 text-left">
        <th className="px-3 py-2 border-b text-xs text-gray-500">Service</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500">Span</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500">Kind</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Offset</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Duration</th>
        <th className="px-3 py-2 border-b text-xs text-gray-500 w-1/3">Timeline</th>
      </tr></thead>
      <tbody>
        {spans.map((s, i) => {
          const offset = s.startTimeMs - t0;
          const leftPct = Math.min(100, (offset / totalDur) * 100);
          const widthPct = Math.max(1, Math.min(100 - leftPct, ((s.durationMs || 0) / totalDur) * 100));
          return (
            <tr key={s.spanId || i} className="hover:bg-gray-50">
              <td className="px-3 py-2 border-b">{s.service || '-'}</td>
              <td className="px-3 py-2 border-b font-medium">{s.name || '-'}</td>
              <td className="px-3 py-2 border-b text-xs text-gray-500">{fmtKind(s.kind)}</td>
              <td className="px-3 py-2 border-b text-right text-xs">{offset} ms</td>
              <td className="px-3 py-2 border-b text-right text-xs">{fmtDuration(s.durationMs)}</td>
              <td className="px-3 py-2 border-b">
                <div className="relative h-3 bg-gray-100 rounded">
                  <div className="absolute h-3 bg-blue-500 rounded" style={{ left: `${leftPct}%`, width: `${widthPct}%` }} />
                </div>
              </td>
            </tr>
          );
        })}
      </tbody>
    </table>
  );
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
