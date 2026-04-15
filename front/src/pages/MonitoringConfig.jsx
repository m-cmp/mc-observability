import { useState, useEffect, useCallback, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { getVmList, getVm, getVmItems, installAgent, uninstallAgent, createVmItem, deleteVmItem } from '../api/vm';
import { getPlugins } from '../api/monitoring';
import { getMci } from '../api/tumblebug';

export default function MonitoringConfig() {
  const { nsId, mciId } = useParams();
  const [vms, setVms] = useState([]);
  const [filter, setFilter] = useState('');
  const [selectedVm, setSelectedVm] = useState(null);
  const [items, setItems] = useState([]);
  const [plugins, setPlugins] = useState([]);
  const [loading, setLoading] = useState(true);
  const [itemLoading, setItemLoading] = useState(false);
  const [busy, setBusy] = useState(false); // block metric toggles
  const [busyMsg, setBusyMsg] = useState('');
  const [globalBusy, setGlobalBusy] = useState(false); // block entire page (install/uninstall)
  const pollRef = useRef(null);

  const loadVms = useCallback(async () => {
    if (!nsId || !mciId) return;
    setLoading(true);
    try {
      const mciData = await getMci(nsId, mciId);
      const tbVms = mciData.vm || [];
      let o11yVms = [];
      try { o11yVms = await getVmList(nsId, mciId); } catch {}
      const o11yMap = {};
      o11yVms.forEach((v) => { o11yMap[v.vm_id || v.id] = v; });
      setVms(tbVms.map((vm) => {
        const o = o11yMap[vm.id] || {};
        return { ...vm, monitoring_agent_status: o.monitoring_agent_status || null, log_agent_status: o.log_agent_status || null, registered: !!o11yMap[vm.id] };
      }));
    } catch { setVms([]); }
    setLoading(false);
  }, [nsId, mciId]);

  useEffect(() => { loadVms(); return () => { if (pollRef.current) clearInterval(pollRef.current); }; }, [loadVms]);
  useEffect(() => { getPlugins().then(setPlugins).catch(() => setPlugins([])); }, []);

  async function selectVm(vm) {
    if (globalBusy) return;
    setSelectedVm(vm);
    setItems([]);
    if (!vm.registered) return;
    setItemLoading(true);
    try { setItems(await getVmItems(nsId, mciId, vm.id)); } catch { setItems([]); }
    setItemLoading(false);
  }

  // --- Install / Uninstall with polling ---
  async function handleInstall(e, vm) {
    e.stopPropagation();
    if (!confirm(`Install monitoring agent on "${vm.name || vm.id}"?`)) return;
    setGlobalBusy(true);
    setBusyMsg(`Installing agent on ${vm.name || vm.id}...`);
    try {
      await installAgent(nsId, mciId, vm.id);
      startPolling(vm.id);
    } catch (err) {
      alert('Install failed: ' + (err.response?.data?.error_message || err.message));
      setGlobalBusy(false);
      setBusyMsg('');
    }
  }

  async function handleUninstall(e, vm) {
    e.stopPropagation();
    if (!confirm(`Uninstall monitoring agent from "${vm.name || vm.id}"?`)) return;
    setGlobalBusy(true);
    setBusyMsg(`Uninstalling agent from ${vm.name || vm.id}...`);
    try {
      await uninstallAgent(nsId, mciId, vm.id);
      await loadVms();
      if (selectedVm?.id === vm.id) { setSelectedVm(null); setItems([]); }
    } catch (err) {
      alert('Uninstall failed: ' + (err.response?.data?.error_message || err.message));
    }
    setGlobalBusy(false);
    setBusyMsg('');
  }

  function startPolling(vmId) {
    if (pollRef.current) clearInterval(pollRef.current);
    let count = 0;
    pollRef.current = setInterval(async () => {
      count++;
      try {
        const vmData = await getVm(nsId, mciId, vmId);
        const status = vmData?.monitoring_agent_status || vmData?.monitoringAgentStatus;
        setBusyMsg(`Installing agent... (${status || 'checking'}) [${count * 5}s]`);
        if (status === 'SUCCESS' || status === 'FAILED' || count > 60) {
          clearInterval(pollRef.current);
          pollRef.current = null;
          setGlobalBusy(false);
          setBusyMsg('');
          await loadVms();
          if (status === 'SUCCESS') {
            // Auto-select the VM to show items
            const refreshedVms = await getVmList(nsId, mciId);
            const found = refreshedVms.find(v => (v.vm_id || v.id) === vmId);
            if (found) selectVm({ ...found, id: vmId, registered: true });
          }
        }
      } catch {
        // Keep polling
      }
    }, 5000);
  }

  // --- Metric toggle with confirmation + overlay ---
  async function handleToggle(plugin, isActive) {
    if (!selectedVm || busy) return;
    const action = isActive ? 'Disable' : 'Enable';
    if (!confirm(`${action} "${plugin.name}" monitoring metric on ${selectedVm.name || selectedVm.id}?`)) return;

    setBusy(true);
    setBusyMsg(`${action === 'Enable' ? 'Enabling' : 'Disabling'} ${plugin.name} metric...`);
    try {
      if (isActive) {
        const item = items.find((it) => it.pluginSeq === plugin.seq);
        if (item) await deleteVmItem(nsId, mciId, selectedVm.id, item.seq);
      } else {
        await createVmItem(nsId, mciId, selectedVm.id, { pluginSeq: plugin.seq });
      }
      setItems(await getVmItems(nsId, mciId, selectedVm.id));
    } catch (err) {
      alert('Failed: ' + (err.response?.data?.error_message || err.message));
    }
    setBusy(false);
    setBusyMsg('');
  }

  const filteredVms = filter ? vms.filter((vm) => (vm.name || vm.id || '').toLowerCase().includes(filter.toLowerCase())) : vms;
  const inputPlugins = plugins.filter((p) => p.pluginType === 'INPUT');
  const activeSeqs = new Set(items.map((it) => it.pluginSeq));

  return (
    <div className="space-y-4 relative">
      {/* Global busy overlay (install/uninstall) */}
      {globalBusy && (
        <div className="fixed inset-0 bg-white/60 backdrop-blur-sm z-50 flex items-center justify-center">
          <div className="bg-white rounded-lg shadow-xl p-6 text-center">
            <div className="w-8 h-8 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-3" />
            <p className="text-sm font-medium text-gray-700">{busyMsg}</p>
            <p className="text-xs text-gray-400 mt-1">Please wait...</p>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold">Monitor Setting / Workload</div>
        <div className="p-4"><span className="text-sm text-gray-600">Workload: </span><span className="font-medium">{mciId}</span></div>
      </div>

      {/* Server list */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex items-center justify-between">
          <span className="font-semibold">List of Servers</span>
          <div className="flex items-center gap-2">
            <input type="text" placeholder="Filter..." value={filter} onChange={(e) => setFilter(e.target.value)} className="border rounded px-2 py-1 text-sm w-40" />
            <button onClick={loadVms} className="text-sm text-gray-500 hover:text-gray-700">Refresh</button>
          </div>
        </div>
        <div className="overflow-auto">
          <table className="w-full text-sm">
            <thead><tr className="bg-gray-50 text-left">
              <th className="px-4 py-2.5 border-b text-gray-500">Name</th>
              <th className="px-4 py-2.5 border-b text-gray-500">VM ID</th>
              <th className="px-4 py-2.5 border-b text-gray-500">Status</th>
              <th className="px-4 py-2.5 border-b text-gray-500">Monitoring Agent</th>
              <th className="px-4 py-2.5 border-b text-gray-500">Log Agent</th>
              <th className="px-4 py-2.5 border-b text-gray-500 text-right">Actions</th>
            </tr></thead>
            <tbody>
              {loading ? <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">Loading...</td></tr>
              : filteredVms.length === 0 ? <tr><td colSpan={6} className="px-4 py-8 text-center text-gray-400">No servers</td></tr>
              : filteredVms.map((vm) => (
                <tr key={vm.id} onClick={() => selectVm(vm)}
                  className={`cursor-pointer hover:bg-blue-50 ${selectedVm?.id === vm.id ? 'bg-blue-100' : ''}`}>
                  <td className="px-4 py-2.5 border-b font-medium">{vm.name || vm.id}</td>
                  <td className="px-4 py-2.5 border-b text-gray-500">{vm.id}</td>
                  <td className="px-4 py-2.5 border-b"><Badge status={vm.status} /></td>
                  <td className="px-4 py-2.5 border-b"><AgentBadge status={vm.monitoring_agent_status} /></td>
                  <td className="px-4 py-2.5 border-b"><AgentBadge status={vm.log_agent_status} /></td>
                  <td className="px-4 py-2.5 border-b text-right">
                    {!vm.registered
                      ? <button onClick={(e) => handleInstall(e, vm)} disabled={busy} className="text-sm bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700 disabled:opacity-50">Install Agent</button>
                      : vm.monitoring_agent_status === 'INSTALLING'
                      ? <span className="text-sm text-yellow-600 animate-pulse">Installing...</span>
                      : <button onClick={(e) => handleUninstall(e, vm)} disabled={busy} className="text-sm text-red-500 hover:text-red-700 disabled:opacity-50">Uninstall</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Metric toggle table */}
      {selectedVm && (
        <div className="bg-white rounded-lg shadow">
          <div className="px-4 py-3 border-b font-semibold">Monitoring Metrics — {selectedVm.name || selectedVm.id}</div>
          <div className="p-4 relative">
            {/* Metric toggle busy overlay */}
            {busy && (
              <div className="absolute inset-0 bg-white/70 backdrop-blur-[2px] z-10 flex items-center justify-center rounded">
                <div className="text-center">
                  <div className="w-6 h-6 border-3 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-2" />
                  <p className="text-sm text-gray-600">{busyMsg}</p>
                </div>
              </div>
            )}
            {!selectedVm.registered ? (
              <div className="text-center py-6">
                <p className="text-sm text-gray-500 mb-3">Agent not installed.</p>
                <button onClick={(e) => handleInstall(e, selectedVm)} disabled={busy} className="bg-blue-600 text-white px-4 py-2 rounded disabled:opacity-50">Install Agent</button>
              </div>
            ) : itemLoading ? (
              <p className="text-sm text-gray-400 animate-pulse">Loading metrics...</p>
            ) : (
              <table className="w-full text-sm">
                <thead><tr className="bg-gray-50 text-left">
                  <th className="px-4 py-2.5 border-b text-gray-500 w-20">Active</th>
                  <th className="px-4 py-2.5 border-b text-gray-500">Metric</th>
                  <th className="px-4 py-2.5 border-b text-gray-500">Plugin ID</th>
                  <th className="px-4 py-2.5 border-b text-gray-500">State</th>
                </tr></thead>
                <tbody>
                  {inputPlugins.map((plugin) => {
                    const isActive = activeSeqs.has(plugin.seq);
                    const item = items.find((it) => it.pluginSeq === plugin.seq);
                    return (
                      <tr key={plugin.seq} className={`hover:bg-gray-50 ${busy ? 'opacity-50 pointer-events-none' : ''}`}>
                        <td className="px-4 py-2.5 border-b">
                          <button onClick={() => handleToggle(plugin, isActive)} disabled={busy}
                            className={`w-11 h-6 rounded-full relative transition-colors ${isActive ? 'bg-blue-600' : 'bg-gray-300'} disabled:opacity-50`}>
                            <span className={`absolute top-1 w-4 h-4 bg-white rounded-full shadow transition-transform ${isActive ? 'left-6' : 'left-1'}`} />
                          </button>
                        </td>
                        <td className="px-4 py-2.5 border-b font-medium">{plugin.name}</td>
                        <td className="px-4 py-2.5 border-b text-gray-500">{plugin.pluginId}</td>
                        <td className="px-4 py-2.5 border-b">
                          {isActive
                            ? <span className="px-2 py-0.5 rounded-full bg-green-100 text-green-700 text-xs">{item?.state || 'ACTIVE'}</span>
                            : <span className="text-gray-400 text-xs">Not configured</span>}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function Badge({ status }) {
  const s = (status || '').toUpperCase();
  const c = s.includes('RUNNING') ? 'bg-green-100 text-green-700' : s === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-500';
  return <span className={`text-xs px-2 py-0.5 rounded-full ${c}`}>{status || '-'}</span>;
}

function AgentBadge({ status }) {
  if (!status) return <span className="text-xs text-gray-400">Not installed</span>;
  const s = status.toUpperCase();
  const c = s === 'SUCCESS' ? 'bg-green-100 text-green-700' : s === 'INSTALLING' ? 'bg-yellow-100 text-yellow-700' :
    s === 'SERVICE_INACTIVE' ? 'bg-orange-100 text-orange-700' : s === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-gray-100 text-gray-500';
  return <span className={`text-xs px-2 py-0.5 rounded-full ${c}`}>{status}</span>;
}
