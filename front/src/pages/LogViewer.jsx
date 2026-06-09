import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { queryLogs } from '../api/logs';
import { getInfra, getInfraList } from '../api/tumblebug';
import LogTable from '../components/LogTable';

export default function LogViewer() {
  const { nsId, infraId, nodeId: routeNodeId } = useParams();

  const [infraList, setInfraList] = useState([]);
  const [selectedInfraId, setSelectedInfraId] = useState(infraId || '');
  const [nodes, setNodes] = useState([]);
  const [selectedNodeId, setSelectedNodeId] = useState(routeNodeId || '');
  const [keyword, setKeyword] = useState('');
  const [logs, setLogs] = useState(null);
  const [loading, setLoading] = useState(false);

  // NS level: load Infra list
  useEffect(() => {
    if (!nsId) return;
    if (!infraId) {
      getInfraList(nsId).then(setInfraList).catch(() => setInfraList([]));
    } else {
      setSelectedInfraId(infraId);
    }
  }, [nsId, infraId]);

  // Load Node list when an Infra is selected
  useEffect(() => {
    if (!nsId || !selectedInfraId) { setNodes([]); return; }
    getInfra(nsId, selectedInfraId)
      .then((data) => setNodes(data.node || []))
      .catch(() => setNodes([]));
  }, [nsId, selectedInfraId]);

  useEffect(() => {
    if (routeNodeId) setSelectedNodeId(routeNodeId);
  }, [routeNodeId]);

  const search = useCallback(async () => {
    if (!nsId || !selectedInfraId) return;
    setLoading(true);
    try {
      const result = await queryLogs({ nsId, infraId: selectedInfraId, nodeId: selectedNodeId, keyword });
      setLogs(Array.isArray(result) ? result : []);
    } catch (e) {
      console.error('Log query failed', e);
      setLogs([]);
    }
    setLoading(false);
  }, [nsId, selectedInfraId, selectedNodeId, keyword]);

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
              {infraId ? (
                <input className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-50" value={infraId} readOnly />
              ) : (
                <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedInfraId} onChange={(e) => { setSelectedInfraId(e.target.value); setSelectedNodeId(''); }}>
                  <option value="">Select Infra</option>
                  {infraList.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
                </select>
              )}
            </div>
            {/* Server */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Server</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedNodeId} onChange={(e) => setSelectedNodeId(e.target.value)}>
                <option value="">All</option>
                {nodes.map((node) => <option key={node.id} value={node.id}>{node.name || node.id}</option>)}
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
              <button onClick={search} disabled={loading || !selectedInfraId}
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
