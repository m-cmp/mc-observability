import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { queryLogs } from '../api/logs';
import { getMci } from '../api/tumblebug';
import LogTable from '../components/LogTable';

export default function LogViewer() {
  const { nsId, mciId, vmId: routeVmId } = useParams();

  const [vms, setVms] = useState([]);
  const [selectedVmId, setSelectedVmId] = useState(routeVmId || '');
  const [keyword, setKeyword] = useState('');
  const [logs, setLogs] = useState(null);
  const [loading, setLoading] = useState(false);

  // Load VM list from Tumblebug
  useEffect(() => {
    if (!nsId || !mciId) return;
    getMci(nsId, mciId)
      .then((data) => setVms(data.vm || []))
      .catch(() => setVms([]));
  }, [nsId, mciId]);

  useEffect(() => {
    if (routeVmId) setSelectedVmId(routeVmId);
  }, [routeVmId]);

  const search = useCallback(async () => {
    if (!nsId || !mciId) return;
    setLoading(true);
    try {
      const result = await queryLogs({ nsId, mciId, vmId: selectedVmId, keyword });
      setLogs(Array.isArray(result) ? result : []);
    } catch (e) {
      console.error('Log query failed', e);
      setLogs([]);
    }
    setLoading(false);
  }, [nsId, mciId, selectedVmId, keyword]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') search();
  };

  return (
    <div className="space-y-4">
      {/* Card: Log Manage */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Log Manage</div>
        <div className="p-4">
          <div className="grid grid-cols-4 gap-4 mb-4">
            {/* Workload (readonly) */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Workload</label>
              <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={mciId || ''} readOnly />
            </div>
            {/* Server */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Server</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedVmId} onChange={(e) => setSelectedVmId(e.target.value)}>
                <option value="">All</option>
                {vms.map((vm) => {
                  const id = vm.id || vm.vm_id || vm.name;
                  return <option key={id} value={id}>{vm.name || id}</option>;
                })}
              </select>
            </div>
            {/* Keyword */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Keyword</label>
              <input
                type="text"
                placeholder="Search keyword..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                onKeyDown={handleKeyDown}
                className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm"
              />
            </div>
            {/* Search button */}
            <div className="flex items-end">
              <button
                onClick={search}
                disabled={loading}
                className="px-4 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm"
              >
                {loading ? 'Searching...' : 'Search'}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Card: Log list */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">List of Log</div>
        <div className="p-4">
          <LogTable logs={logs} />
        </div>
      </div>
    </div>
  );
}
