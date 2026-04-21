import { useState, useEffect, useCallback } from 'react';
import { NavLink, Outlet, useParams, useNavigate, useLocation } from 'react-router-dom';
import { setApiToken } from '../api/client';
import { getNsList, getMciList, getMci } from '../api/tumblebug';

const navItems = [
  { label: 'Monitoring', path: 'monitoring' },
  { label: 'Logs', path: 'logs' },
  { label: 'Config', path: 'config' },
  { label: 'Insight', path: 'insight' },
  { label: 'Alerts', path: 'alerts' },
];

export default function Layout() {
  const { nsId, mciId, vmId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();

  const currentSection = navItems.find((n) => location.pathname.includes(`/${n.path}/`))?.path || 'monitoring';

  const [showDevTools, setShowDevTools] = useState(false);
  const [bypassAuth, setBypassAuth] = useState(true);

  const [nsList, setNsList] = useState([]);
  const [mciList, setMciList] = useState([]);
  const [vmList, setVmList] = useState([]);

  useEffect(() => {
    if (bypassAuth) setApiToken('bypass');
  }, [bypassAuth]);

  // Load NS list
  useEffect(() => {
    getNsList().then(setNsList).catch(() => setNsList([]));
  }, []);

  // NS 변경 → MCI 목록 갱신
  const loadMciList = useCallback(async (ns) => {
    if (!ns) { setMciList([]); return []; }
    try {
      const list = await getMciList(ns);
      const arr = Array.isArray(list) ? list : [];
      setMciList(arr);
      return arr;
    } catch { setMciList([]); return []; }
  }, []);

  // MCI 변경 → VM 목록 갱신
  const loadVmList = useCallback(async (ns, mci) => {
    if (!ns || !mci) { setVmList([]); return; }
    try {
      const data = await getMci(ns, mci);
      setVmList(data.vm || []);
    } catch { setVmList([]); }
  }, []);

  // URL의 nsId/mciId 가 바뀔 때마다 목록 갱신
  useEffect(() => { loadMciList(nsId); }, [nsId, loadMciList]);
  useEffect(() => { loadVmList(nsId, mciId); }, [nsId, mciId, loadVmList]);

  // NS 드롭다운 변경
  async function handleNsChange(newNs) {
    const mcis = await loadMciList(newNs);
    const firstMci = mcis[0]?.id;
    if (firstMci) {
      navigate(`/${currentSection}/${newNs}/${firstMci}`);
    }
  }

  // MCI 드롭다운 변경
  function handleMciChange(newMci) {
    if (!newMci) return;
    navigate(`/${currentSection}/${nsId}/${newMci}`);
  }

  // VM 드롭다운 변경
  function handleVmChange(newVm) {
    if (!nsId || !mciId) return;
    let path = `/${currentSection}/${nsId}/${mciId}`;
    if (newVm) path += `/${newVm}`;
    navigate(path);
  }

  const buildPath = (base) => {
    if (mciId) return `/${base}/${nsId}/${mciId}`;
    return `/${base}/${nsId}`;
  };

  return (
    <div className="flex flex-col h-screen">
      <nav className="flex items-center gap-1 px-4 py-2 bg-white border-b border-gray-200 text-sm shrink-0 flex-wrap">
        <a href="/" className="font-semibold text-gray-700 mr-3 hover:text-blue-600">MC-Observability</a>

        {navItems.map((item) => (
          <NavLink key={item.path} to={buildPath(item.path)}
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
          {mciId && <>
            <span className="text-gray-300">/</span>
            {/* MCI */}
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value={mciId || ''}
              onChange={(e) => handleMciChange(e.target.value)}>
              <option value="">MCI</option>
              {mciList.map((m) => <option key={m.id} value={m.id}>{m.name || m.id}</option>)}
            </select>
            <span className="text-gray-300">/</span>
            {/* VM */}
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value={vmId || ''}
              onChange={(e) => handleVmChange(e.target.value)}>
              <option value="">(Overview)</option>
              {vmList.map((vm) => <option key={vm.id} value={vm.id}>{vm.name || vm.id}</option>)}
            </select>
          </>}
          {!mciId && mciList.length > 0 && <>
            <span className="text-gray-300">/</span>
            <select className="border border-gray-200 rounded px-2 py-1 text-xs" value=""
              onChange={(e) => { if (e.target.value) navigate(`/${currentSection}/${nsId}/${e.target.value}`); }}>
              <option value="">MCI</option>
              {mciList.map((m) => <option key={m.id} value={m.id}>{m.name || m.id}</option>)}
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
            <span>Path: <code className="text-green-400">/{currentSection}/{nsId}/{mciId}{vmId ? '/' + vmId : ''}</code></span>
          </div>
          <div className="border-t border-gray-700 pt-2">
            <p className="text-yellow-300 font-semibold mb-1">iframe Integration</p>
            <code className="block bg-gray-900 p-2 rounded text-green-300 whitespace-pre-wrap">{`<iframe src="${window.location.origin}/embed/${currentSection}/${nsId || '{nsId}'}/${mciId || '{mciId}'}${vmId ? '/' + vmId : ''}" />

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
