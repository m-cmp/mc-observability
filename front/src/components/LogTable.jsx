import { useState, useMemo } from 'react';

const PAGE_SIZE = 20;

export default function LogTable({ logs }) {
  const [selected, setSelected] = useState(null);
  const [page, setPage] = useState(0);

  const totalPages = Math.max(1, Math.ceil((logs?.length || 0) / PAGE_SIZE));
  const paged = useMemo(() => (logs || []).slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE), [logs, page]);

  // Reset page when logs change
  useMemo(() => setPage(0), [logs]);

  if (logs === null) {
    return <p className="text-sm text-gray-400 py-4">Click Search to query logs</p>;
  }
  if (!logs || logs.length === 0) {
    return <p className="text-sm text-gray-400 py-4">No logs found</p>;
  }

  return (
    <div className="space-y-3">
      <div className="flex gap-4">
        {/* Table */}
        <div className="flex-1 overflow-auto max-h-[600px]">
          <table className="w-full border-collapse">
            <thead>
              <tr className="bg-gray-100 sticky top-0">
                <th className="text-left px-3 py-2 border-b text-sm font-medium text-gray-600">Timestamp</th>
                <th className="text-left px-3 py-2 border-b text-sm font-medium text-gray-600">Level</th>
                <th className="text-left px-3 py-2 border-b text-sm font-medium text-gray-600">VM</th>
                <th className="text-left px-3 py-2 border-b text-sm font-medium text-gray-600">Service</th>
                <th className="text-left px-3 py-2 border-b text-sm font-medium text-gray-600">Message</th>
              </tr>
            </thead>
            <tbody>
              {paged.map((log, i) => (
                <tr key={page * PAGE_SIZE + i} onClick={() => setSelected(log)}
                  className={`cursor-pointer hover:bg-blue-50 ${selected === log ? 'bg-blue-100' : ''}`}>
                  <td className="px-3 py-2 border-b whitespace-nowrap text-sm text-gray-500">{log.timestamp || '-'}</td>
                  <td className="px-3 py-2 border-b text-sm"><LevelBadge level={log.level} /></td>
                  <td className="px-3 py-2 border-b whitespace-nowrap text-sm">{log.vm_id || '-'}</td>
                  <td className="px-3 py-2 border-b whitespace-nowrap text-sm">{log.service || '-'}</td>
                  <td className="px-3 py-2 border-b text-sm truncate max-w-lg">{log.message || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Detail panel */}
        {selected && (
          <div className="w-96 bg-gray-50 rounded-md p-4 overflow-auto max-h-[600px] shrink-0 text-sm">
            <h4 className="font-semibold mb-3">Log Detail</h4>
            <div className="space-y-1.5 mb-3">
              <Row label="Timestamp" value={selected.timestamp} />
              <Row label="Level" value={selected.level} />
              <Row label="VM" value={selected.vm_id} />
              <Row label="Host" value={selected.host} />
              <Row label="Service" value={selected.service} />
              <Row label="Source" value={selected.source} />
            </div>
            <h4 className="font-semibold mb-1">Message</h4>
            <pre className="whitespace-pre-wrap break-all text-gray-700 bg-white p-3 rounded border text-sm leading-relaxed">
              {selected.message}
            </pre>
            <h4 className="font-semibold mt-3 mb-1">Labels</h4>
            <pre className="whitespace-pre-wrap break-all text-gray-500 bg-white p-3 rounded border text-xs">
              {JSON.stringify(selected.labels, null, 2)}
            </pre>
          </div>
        )}
      </div>

      {/* Pagination */}
      <div className="flex items-center justify-between text-sm">
        <span className="text-gray-500">{logs.length} logs total</span>
        <div className="flex items-center gap-1">
          <button onClick={() => setPage(0)} disabled={page === 0}
            className="px-2 py-1 border rounded disabled:opacity-30 hover:bg-gray-100">&laquo;</button>
          <button onClick={() => setPage(page - 1)} disabled={page === 0}
            className="px-2 py-1 border rounded disabled:opacity-30 hover:bg-gray-100">&lsaquo;</button>
          {pageNumbers(page, totalPages).map((p) =>
            p === '...' ? (
              <span key={`dots-${Math.random()}`} className="px-1 text-gray-400">...</span>
            ) : (
              <button key={p} onClick={() => setPage(p)}
                className={`px-2.5 py-1 border rounded ${p === page ? 'bg-blue-600 text-white border-blue-600' : 'hover:bg-gray-100'}`}>
                {p + 1}
              </button>
            )
          )}
          <button onClick={() => setPage(page + 1)} disabled={page >= totalPages - 1}
            className="px-2 py-1 border rounded disabled:opacity-30 hover:bg-gray-100">&rsaquo;</button>
          <button onClick={() => setPage(totalPages - 1)} disabled={page >= totalPages - 1}
            className="px-2 py-1 border rounded disabled:opacity-30 hover:bg-gray-100">&raquo;</button>
        </div>
      </div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="flex">
      <span className="text-gray-500 w-24 shrink-0">{label}:</span>
      <span className="text-gray-800">{value || '-'}</span>
    </div>
  );
}

function pageNumbers(current, total) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i);
  const pages = [];
  pages.push(0);
  if (current > 3) pages.push('...');
  for (let i = Math.max(1, current - 1); i <= Math.min(total - 2, current + 1); i++) pages.push(i);
  if (current < total - 4) pages.push('...');
  pages.push(total - 1);
  return pages;
}

function LevelBadge({ level }) {
  const l = (level || '').toUpperCase();
  const c = l === 'ERROR' || l === 'FATAL' ? 'bg-red-100 text-red-700' :
    l === 'WARNING' || l === 'WARN' ? 'bg-yellow-100 text-yellow-700' :
    l === 'INFO' ? 'bg-blue-100 text-blue-700' : 'bg-gray-100 text-gray-500';
  return <span className={`px-2 py-0.5 rounded text-sm ${c}`}>{level || '-'}</span>;
}
