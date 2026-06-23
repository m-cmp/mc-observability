import { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { queryLogs } from '../api/logs';
import { getInfra, getInfraList } from '../api/tumblebug';
import { getK8sClusters, getK8sLogStatus } from '../api/k8sAgent';
import LogTable from '../components/LogTable';

export default function LogViewer() {
  const { nsId, infraId, nodeId: routeNodeId } = useParams();

  const [infraList, setInfraList] = useState([]);   // VM infras
  const [clusters, setClusters] = useState([]);     // K8s clusters
  const [selectedInfraId, setSelectedInfraId] = useState(infraId || '');
  const [nodes, setNodes] = useState([]);
  const [nodesLoading, setNodesLoading] = useState(false);
  const [selectedNodeId, setSelectedNodeId] = useState(routeNodeId || '');
  const [keyword, setKeyword] = useState('');
  const [logs, setLogs] = useState(null);
  const [loading, setLoading] = useState(false);

  // The Workload is locked when the route carries an infraId; otherwise it's a dropdown.
  const locked = !!infraId;
  const isK8s = clusters.some((c) => c.id === selectedInfraId);

  // Always load both VM infras and K8s clusters so we can populate the dropdown and
  // know which kind the selected target is (for node loading + Loki labels are shared).
  useEffect(() => {
    if (!nsId) { setInfraList([]); setClusters([]); return; }
    getInfraList(nsId).then((l) => setInfraList(Array.isArray(l) ? l : [])).catch(() => setInfraList([]));
    getK8sClusters(nsId).then((l) => setClusters(Array.isArray(l) ? l : [])).catch(() => setClusters([]));
  }, [nsId]);

  useEffect(() => { if (infraId) setSelectedInfraId(infraId); }, [infraId]);

  // Load Node list when a target is selected — VM nodes from Tumblebug, K8s nodes from
  // the log-agent status (only nodes with a running fluent-bit agent).
  useEffect(() => {
    if (!nsId || !selectedInfraId) { setNodes([]); return; }
    let alive = true;
    setNodesLoading(true);
    const done = () => { if (alive) setNodesLoading(false); };
    if (isK8s) {
      getK8sLogStatus(nsId, selectedInfraId)
        .then((st) => { if (alive) setNodes((Array.isArray(st) ? st : []).filter((n) => n.running).map((n) => ({ id: n.node, name: n.node }))); })
        .catch(() => { if (alive) setNodes([]); })
        .finally(done);
    } else {
      getInfra(nsId, selectedInfraId)
        .then((data) => { if (alive) setNodes(data.node || []); })
        .catch(() => { if (alive) setNodes([]); })
        .finally(done);
    }
    return () => { alive = false; };
  }, [nsId, selectedInfraId, isK8s]);

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

  // When locked, make sure the selected infra still shows a readable label even if the
  // lists haven't resolved it (e.g. it isn't in this user's VM/cluster lists).
  const lockedLabel =
    infraList.find((i) => i.id === selectedInfraId)?.name
    || clusters.find((c) => c.id === selectedInfraId)?.name
    || selectedInfraId;

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Log Manage</div>
        <div className="p-4">
          <div className="grid grid-cols-4 gap-4 mb-4">
            {/* Workload — locked to the route infraId, or a dropdown of VM infras + K8s clusters */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Workload</label>
              {locked ? (
                <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm bg-gray-100 text-gray-600" value={selectedInfraId} disabled>
                  <option value={selectedInfraId}>{lockedLabel}</option>
                </select>
              ) : (
                <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedInfraId} onChange={(e) => { setSelectedInfraId(e.target.value); setSelectedNodeId(''); }}>
                  <option value="">Select Infra / Cluster</option>
                  {infraList.length > 0 && (
                    <optgroup label="VM Infra">
                      {infraList.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
                    </optgroup>
                  )}
                  {clusters.length > 0 && (
                    <optgroup label="K8s Cluster">
                      {clusters.map((c) => <option key={c.id} value={c.id}>{c.name || c.id}</option>)}
                    </optgroup>
                  )}
                </select>
              )}
            </div>
            {/* Server */}
            <div>
              <label className="block text-xs font-medium text-gray-600 mb-1">Server</label>
              <select className="w-full border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedNodeId} onChange={(e) => setSelectedNodeId(e.target.value)}>
                <option value="">{nodesLoading ? 'Loading nodes…' : 'All'}</option>
                {nodesLoading ? <option disabled>Loading nodes…</option> : nodes.map((node) => <option key={node.id} value={node.id}>{node.name || node.id}</option>)}
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
