import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { queryLogs } from '../api/logs';
import { getMci, getMciList } from '../api/tumblebug';
import LogTable from '../components/LogTable';

export default function LogViewer() {
  const { nsId, mciId, vmId: routeVmId } = useParams();

  const [mciList, setMciList] = useState([]);
  const [selectedMciId, setSelectedMciId] = useState(mciId || '');
  const [vms, setVms] = useState([]);
  const [selectedVmId, setSelectedVmId] = useState(routeVmId || '');
  const [keyword, setKeyword] = useState('');
  const [logs, setLogs] = useState(null);
  const [loading, setLoading] = useState(false);

  // NS 레벨: MCI 목록 로드
  useEffect(() => {
    if (!nsId) return;
    if (!mciId) {
      getMciList(nsId).then(setMciList).catch(() => setMciList([]));
    } else {
      setSelectedMciId(mciId);
    }
  }, [nsId, mciId]);

  // MCI 선택 시 VM 목록 로드
  useEffect(() => {
    if (!nsId || !selectedMciId) { setVms([]); return; }
    getMci(nsId, selectedMciId)
      .then((data) => setVms(data.vm || []))
      .catch(() => setVms([]));
  }, [nsId, selectedMciId]);

  useEffect(() => {
    if (routeVmId) setSelectedVmId(routeVmId);
  }, [routeVmId]);

  const search = useCallback(async () => {
    if (!nsId || !selectedMciId) return;
    setLoading(true);
    try {
      const result = await queryLogs({ nsId, mciId: selectedMciId, vmId: selectedVmId, keyword });
      setLogs(Array.isArray(result) ? result : []);
    } catch (e) {
      console.error('Log query failed', e);
      setLogs([]);
    }
    setLoading(false);
  }, [nsId, selectedMciId, selectedVmId, keyword]);

  const handleKeyDown = (e) => {
    if (e.key === 'Enter') search();
  };

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Log Manage</div>
        <div className="p-4">
          <div className="grid grid-cols-4 gap-4 mb-4">
            {/* Workload */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Workload</label>
              {mciId ? (
                <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={mciId} readOnly />
              ) : (
                <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedMciId} onChange={(e) => { setSelectedMciId(e.target.value); setSelectedVmId(''); }}>
                  <option value="">Select MCI</option>
                  {mciList.map((m) => <option key={m.id} value={m.id}>{m.name || m.id}</option>)}
                </select>
              )}
            </div>
            {/* Server */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Server</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedVmId} onChange={(e) => setSelectedVmId(e.target.value)}>
                <option value="">All</option>
                {vms.map((vm) => <option key={vm.id} value={vm.id}>{vm.name || vm.id}</option>)}
              </select>
            </div>
            {/* Keyword */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Keyword</label>
              <input type="text" placeholder="Search keyword..." value={keyword} onChange={(e) => setKeyword(e.target.value)} onKeyDown={handleKeyDown}
                className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" />
            </div>
            {/* Search */}
            <div className="flex items-end">
              <button onClick={search} disabled={loading || !selectedMciId}
                className="px-4 py-1.5 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50 text-sm">
                {loading ? 'Searching...' : 'Search'}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">List of Log</div>
        <div className="p-4">
          <LogTable logs={logs} />
        </div>
      </div>
    </div>
  );
}
