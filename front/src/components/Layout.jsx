import { useState, useEffect, useCallback } from 'react';
import { NavLink, Outlet, useParams, useNavigate, useLocation } from 'react-router-dom';
import useBasePath from '../hooks/useBasePath';
import { setApiToken } from '../api/client';
import { getInfraList, getInfra } from '../api/tumblebug';

// Idle = black text (no color). Active = per-feature colored background + white
// text. Explicit class strings so Tailwind keeps them.
const IDLE = 'text-gray-800 hover:bg-gray-100';
const navItems = [
  { label: 'Monitoring', path: 'monitoring', active: 'bg-blue-600 text-white' },
  { label: 'Logs', path: 'logs', active: 'bg-emerald-600 text-white' },
  { label: 'Config', path: 'config', active: 'bg-amber-600 text-white' },
  { label: 'Insight', path: 'insight', active: 'bg-purple-600 text-white' },
  { label: 'Alerts', path: 'alerts', active: 'bg-red-600 text-white' },
  { label: 'Tracing', path: 'trace', active: 'bg-teal-600 text-white' },
];

export default function Layout() {
  const { nsId, infraId, nodeId } = useParams();
  const navigate = useNavigate();
  const base = useBasePath();
  const location = useLocation();

  const currentSection = navItems.find((n) => location.pathname.includes(`/${n.path}/`))?.path || 'monitoring';

  const [bypassAuth] = useState(true);

  const [infraList, setInfraList] = useState([]);
  const [nodeList, setNodeList] = useState([]);

  useEffect(() => {
    if (bypassAuth) setApiToken('bypass');
  }, [bypassAuth]);

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

  // Node dropdown change
  function handleNodeChange(newNode) {
    if (!nsId || !infraId) return;
    let path = `${base}/${currentSection}/${nsId}/${infraId}`;
    if (newNode) path += `/${newNode}`;
    navigate(path);
  }

  // Top menu always navigates to the namespace level for the section,
  // dropping any selected infra/node.
  const buildNavPath = (section) => {
    if (!nsId) return `${base}/`;
    return `${base}/${section}/${nsId}`;
  };

  return (
    <div className="flex flex-col h-screen">
      <nav className="flex items-center gap-1 px-4 py-2 bg-white border-b border-gray-200 text-sm shrink-0 flex-wrap">
        {nsId ? (
          navItems.map((item) => (
            <NavLink key={item.path} to={buildNavPath(item.path)}
              className={`px-3 py-1.5 rounded-md transition-colors ${currentSection === item.path ? item.active : IDLE}`}>
              {item.label}
            </NavLink>
          ))
        ) : (
          <>
            {navItems.map((item) => (
              <span key={item.path}
                className="px-3 py-1.5 rounded-md text-gray-300 cursor-not-allowed select-none">
                {item.label}
              </span>
            ))}
            <span className="ml-2 text-xs text-gray-400">← Select a namespace</span>
          </>
        )}

        <div className="ml-auto flex items-center gap-1">
          {/* Infra selector — shown only when an infra is NOT yet in the path */}
          {nsId && !infraId && infraList.length > 0 && (
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value=""
              onChange={(e) => { if (e.target.value) navigate(`${base}/${currentSection}/${nsId}/${e.target.value}`); }}>
              <option value="">Infra</option>
              {infraList.map((i) => <option key={i.id} value={i.id}>{i.name || i.id}</option>)}
            </select>
          )}
          {/* Node selector — when an infra is in the path */}
          {infraId && (
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value={nodeId || ''}
              onChange={(e) => handleNodeChange(e.target.value)}>
              <option value="">(Overview)</option>
              {nodeList.map((node) => <option key={node.id} value={node.id}>{node.name || node.id}</option>)}
            </select>
          )}
        </div>
      </nav>

      <main className="flex-1 overflow-auto p-4 bg-gray-50">
        <Outlet />
      </main>
    </div>
  );
}
