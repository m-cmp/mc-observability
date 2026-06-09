import { useState, useEffect, useCallback } from 'react';
import { NavLink, Outlet, useParams, useNavigate, useLocation } from 'react-router-dom';
import useBasePath from '../hooks/useBasePath';
import { setApiToken } from '../api/client';
import { getNsList, getInfraList, getInfra } from '../api/tumblebug';

const navItems = [
  { label: 'Monitoring', path: 'monitoring' },
  { label: 'Logs', path: 'logs' },
  { label: 'Config', path: 'config' },
  { label: 'Insight', path: 'insight' },
  { label: 'Alerts', path: 'alerts' },
  { label: 'Tracing', path: 'trace' },
];

export default function Layout() {
  const { nsId, infraId, nodeId } = useParams();
  const navigate = useNavigate();
  const base = useBasePath();
  const location = useLocation();

  const currentSection = navItems.find((n) => location.pathname.includes(`/${n.path}/`))?.path || 'monitoring';

  const [showDevTools, setShowDevTools] = useState(false);
  const [bypassAuth, setBypassAuth] = useState(true);

  const [nsList, setNsList] = useState([]);
  const [infraList, setInfraList] = useState([]);
  const [nodeList, setNodeList] = useState([]);

  useEffect(() => {
    if (bypassAuth) setApiToken('bypass');
  }, [bypassAuth]);

  // Load NS list
  useEffect(() => {
    getNsList().then(setNsList).catch(() => setNsList([]));
  }, []);

  // NS change -> refresh Infra list
  const loadInfraList = useCallback(async (ns) => {
    if (!ns) { setInfraList([]); return []; }
    try {
      const list = await getInfraList(ns);
      const arr = Array.isArray(list) ? list : [];
      setInfraList(arr);
      return arr;
    } catch { setInfraList([]); return []; }
  }, []);

  // Infra change -> refresh Node list
  const loadNodeList = useCallback(async (ns, infra) => {
    if (!ns || !infra) { setNodeList([]); return; }
    try {
      const data = await getInfra(ns, infra);
      setNodeList(data.node || []);
    } catch { setNodeList([]); }
  }, []);

  // Refresh lists whenever URL nsId/infraId changes
  useEffect(() => { loadInfraList(nsId); }, [nsId, loadInfraList]);
  useEffect(() => { loadNodeList(nsId, infraId); }, [nsId, infraId, loadNodeList]);

  // NS dropdown change
  async function handleNsChange(newNs) {
    const infras = await loadInfraList(newNs);
    const firstInfra = infras[0]?.id;
    if (firstInfra) {
      navigate(`${base}/${currentSection}/${newNs}/${firstInfra}`);
    }
  }

  // Infra dropdown change
  function handleInfraChange(newInfra) {
    if (!newInfra) {
      navigate(`${base}/${currentSection}/${nsId}`);
      return;
    }
    navigate(`${base}/${currentSection}/${nsId}/${newInfra}`);
  }

  // Node dropdown change
  function handleNodeChange(newNode) {
    if (!nsId || !infraId) return;
    let path = `${base}/${currentSection}/${nsId}/${infraId}`;
    if (newNode) path += `/${newNode}`;
    navigate(path);
  }

  const buildNavPath = (section) => {
    if (infraId) return `${base}/${section}/${nsId}/${infraId}`;
    return `${base}/${section}/${nsId}`;
  };

  return (
    <div className="flex flex-col h-screen">
      <nav className="flex items-center gap-1 px-4 py-2 bg-white border-b border-gray-200 text-sm shrink-0 flex-wrap">
        <a href="/" className="font-semibold text-gray-700 mr-3 hover:text-blue-600">MC-Observability</a>

        {navItems.map((item) => (
          <NavLink key={item.path} to={buildNavPath(item.path)}
            className={({ isActive }) =>
              `px-3 py-1.5 rounded-md transition-colors ${isActive ? 'bg-blue-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`
            }>
            {item.label}
          </NavLink>
        ))}

        <div className="ml-auto flex items-center gap-1">
          {/* NS */}
          <select className="border border-gray-200 rounded px-2 py-1 text-xs" value={nsId || ''}
            onChange={(e) => handleNsChange(e.target.value)}>
            <option value="">NS</option>
            {nsList.map((ns) => <option key={ns.id} value={ns.id}>{ns.id}</option>)}
          </select>
          {infraId && <>
            <span className="text-gray-300">/</span>
            {/* Infra */}
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value={infraId || ''}
              onChange={(e) => handleInfraChange(e.target.value)}>
              <option value="">Infra</option>
              {infraList.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
            </select>
            <span className="text-gray-300">/</span>
            {/* Node */}
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value={nodeId || ''}
              onChange={(e) => handleNodeChange(e.target.value)}>
              <option value="">(Overview)</option>
              {nodeList.map((node) => <option key={node.id} value={node.id}>{node.name || node.id}</option>)}
            </select>
          </>}
          {!infraId && infraList.length > 0 && <>
            <span className="text-gray-300">/</span>
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value=""
              onChange={(e) => { if (e.target.value) navigate(`${base}/${currentSection}/${nsId}/${e.target.value}`); }}>
              <option value="">Infra</option>
              {infraList.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
            </select>
          </>}

          <button onClick={() => setShowDevTools(!showDevTools)}
            className={`ml-2 text-xs border rounded px-2 py-1 ${showDevTools ? 'bg-gray-800 text-white border-gray-800' : 'text-gray-400 border-gray-200 hover:text-gray-600'}`}>
            Dev
          </button>
        </div>
      </nav>

      {showDevTools && (
        <div className="bg-gray-800 text-gray-200 px-4 py-3 text-xs space-y-2 shrink-0">
          <div className="flex items-center gap-4">
            <label className="flex items-center gap-1 cursor-pointer">
              <input type="checkbox" checked={bypassAuth} onChange={(e) => { setBypassAuth(e.target.checked); if (e.target.checked) setApiToken('bypass'); }} />
              Token Bypass
            </label>
            <span className="text-gray-500">|</span>
            <span>Path: <code className="text-green-400">/{currentSection}/{nsId}/{infraId}{nodeId ? '/' + nodeId : ''}</code></span>
          </div>
          <div className="border-t border-gray-700 pt-2">
            <p className="text-yellow-300 font-semibold mb-1">iframe Integration</p>
            <code className="block bg-gray-900 p-2 rounded text-green-300 whitespace-pre-wrap">{`<iframe src="${window.location.origin}/embed/${currentSection}/${nsId || '{nsId}'}/${infraId || '{infraId}'}${nodeId ? '/' + nodeId : ''}" />

<script>
iframe.onload = () => iframe.contentWindow.postMessage({
  accessToken: "Bearer ...",
  projectInfo: { ns_id: "${nsId || 'ns-1'}" }
}, "${window.location.origin}");
</script>`}</code>
          </div>
        </div>
      )}

      <main className="flex-1 overflow-auto p-4 bg-gray-50">
        <Outlet />
      </main>
    </div>
  );
}
