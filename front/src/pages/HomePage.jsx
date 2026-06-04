import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAppContext } from '../context/AppContext';
import { setApiToken } from '../api/client';
import { getNsList, getInfraList, getInfra } from '../api/tumblebug';

export default function HomePage() {
  const navigate = useNavigate();
  const { token } = useAppContext();

  const [bypassAuth, setBypassAuth] = useState(true);
  const [manualToken, setManualToken] = useState('');

  // Tumblebug cascade
  const [nsList, setNsList] = useState([]);
  const [nsId, setNsId] = useState('');
  const [infraList, setInfraList] = useState([]);
  const [infraId, setInfraId] = useState('');
  const [nodeList, setNodeList] = useState([]);
  const [nodeId, setNodeId] = useState('');
  const [loadingNs, setLoadingNs] = useState(false);
  const [loadingInfra, setLoadingInfra] = useState(false);
  const [loadingNode, setLoadingNode] = useState(false);

  useEffect(() => {
    if (token) setManualToken(token);
  }, [token]);

  // Apply bypass on mount and whenever toggle changes
  useEffect(() => {
    if (bypassAuth) setApiToken('bypass');
    else if (manualToken) setApiToken(manualToken);
  }, [bypassAuth, manualToken]);

  // Load NS list on mount
  useEffect(() => {
    setLoadingNs(true);
    getNsList()
      .then(setNsList)
      .catch((e) => { console.error('NS load failed', e); setNsList([]); })
      .finally(() => setLoadingNs(false));
  }, [bypassAuth]);

  // Load Infra when NS changes
  useEffect(() => {
    setInfraList([]);
    setInfraId('');
    setNodeList([]);
    setNodeId('');
    if (!nsId) return;
    setLoadingInfra(true);
    getInfraList(nsId)
      .then((list) => setInfraList(Array.isArray(list) ? list : []))
      .catch(() => setInfraList([]))
      .finally(() => setLoadingInfra(false));
  }, [nsId]);

  // Load Nodes when Infra changes
  useEffect(() => {
    setNodeList([]);
    setNodeId('');
    if (!nsId || !infraId) return;
    setLoadingNode(true);
    getInfra(nsId, infraId)
      .then((data) => setNodeList(data.node || []))
      .catch(() => setNodeList([]))
      .finally(() => setLoadingNode(false));
  }, [nsId, infraId]);

  function go(page) {
    if (bypassAuth) {
      setApiToken('bypass');
    } else if (manualToken) {
      setApiToken(manualToken);
    }
    if (!nsId || !infraId) {
      alert('Namespace and Infra are required.');
      return;
    }
    let path = `/${page}/${nsId}/${infraId}`;
    if (nodeId) path += `/${nodeId}`;
    navigate(path);
  }

  return (
    <div className="min-h-screen bg-gray-50 flex items-center justify-center">
      <div className="bg-white rounded-lg shadow-lg p-8 w-full max-w-xl">
        <h1 className="text-xl font-bold mb-1">MC-Observability</h1>
        <p className="text-sm text-gray-500 mb-6">Standalone Test Console</p>

        {/* Auth */}
        <div className="mb-4">
          <label className="flex items-center gap-2 text-sm mb-2 cursor-pointer">
            <input type="checkbox" checked={bypassAuth} onChange={(e) => setBypassAuth(e.target.checked)} className="rounded" />
            Bypass Authentication
          </label>
          {!bypassAuth && (
            <input type="text" placeholder="Bearer token..." value={manualToken} onChange={(e) => setManualToken(e.target.value)}
              className="w-full border border-gray-300 rounded px-3 py-2 text-sm font-mono" />
          )}
        </div>

        {/* Tumblebug cascade selectors */}
        <div className="space-y-3 mb-6">
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Namespace (ns_id) *</label>
            <select className="w-full border border-gray-300 rounded px-3 py-2 text-sm" value={nsId} onChange={(e) => setNsId(e.target.value)} disabled={loadingNs}>
              <option value="">{loadingNs ? 'Loading...' : 'Select Namespace'}</option>
              {nsList.map((ns) => <option key={ns.id} value={ns.id}>{ns.name || ns.id}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Infra (infra_id) *</label>
            <select className="w-full border border-gray-300 rounded px-3 py-2 text-sm" value={infraId} onChange={(e) => setInfraId(e.target.value)} disabled={!nsId || loadingInfra}>
              <option value="">{loadingInfra ? 'Loading...' : 'Select Infra'}</option>
              {infraList.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-medium text-gray-600 mb-1">Node (node_id) — optional</label>
            <select className="w-full border border-gray-300 rounded px-3 py-2 text-sm" value={nodeId} onChange={(e) => setNodeId(e.target.value)} disabled={!infraId || loadingNode}>
              <option value="">{loadingNode ? 'Loading...' : 'All Nodes'}</option>
              {nodeList.map((node) => <option key={node.id} value={node.id}>{node.name || node.id} ({node.status})</option>)}
            </select>
          </div>
        </div>

        {/* Nav buttons */}
        <div className="grid grid-cols-2 gap-3">
          <button onClick={() => go('monitoring')} className="bg-blue-600 text-white rounded py-2.5 text-sm font-medium hover:bg-blue-700">
            Monitoring
          </button>
          <button onClick={() => go('logs')} className="bg-emerald-600 text-white rounded py-2.5 text-sm font-medium hover:bg-emerald-700">
            Logs
          </button>
          <button onClick={() => go('config')} className="bg-amber-600 text-white rounded py-2.5 text-sm font-medium hover:bg-amber-700">
            Monitoring Config
          </button>
          <button onClick={() => go('insight')} className="bg-purple-600 text-white rounded py-2.5 text-sm font-medium hover:bg-purple-700">
            Insight
          </button>
          <button onClick={() => go('alerts')} className="bg-red-600 text-white rounded py-2.5 text-sm font-medium hover:bg-red-700">
            Alerts
          </button>
          <button onClick={() => {
            if (!nsId || !infraId) { alert('Select NS and Infra first'); return; }
            window.open(nodeId ? `/embed/monitoring/${nsId}/${infraId}/${nodeId}` : `/embed/monitoring/${nsId}/${infraId}`, '_blank');
          }} className="bg-gray-600 text-white rounded py-2.5 text-sm font-medium hover:bg-gray-700">
            Embed Preview
          </button>
        </div>

        <p className="mt-4 text-xs text-gray-400">NS/Infra/Node are loaded from Tumblebug. /embed/* routes have no nav (iframe mode).</p>
      </div>
    </div>
  );
}
