import { useState, useEffect } from 'react';
import { getPolicies, createPolicy, deletePolicy, addVmToPolicy } from '../api/trigger';
import { getNotiChannels, getNotiHistory } from '../api/trigger';
import { useParams } from 'react-router-dom';

const TABS = ['Policies', 'Notification History'];
const RESOURCE_TYPES = ['CPU', 'MEMORY', 'DISK'];
const AGG_TYPES = ['AVG', 'MAX', 'MIN', 'LAST'];

export default function AlertManager() {
  const { nsId, mciId } = useParams();
  const [tab, setTab] = useState(0);

  return (
    <div className="space-y-4">
      <div className="flex gap-1 bg-white rounded-lg shadow px-2 py-1">
        {TABS.map((t, i) => (
          <button key={t} onClick={() => setTab(i)}
            className={`px-4 py-2 text-sm rounded ${tab === i ? 'bg-red-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}>
            {t}
          </button>
        ))}
      </div>
      {tab === 0 && <PoliciesTab nsId={nsId} mciId={mciId} />}
      {tab === 1 && <NotiHistoryTab />}
    </div>
  );
}

function PoliciesTab({ nsId, mciId }) {
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);

  const load = () => {
    setLoading(true);
    getPolicies(1, 50).then(data => setPolicies(data.content || []))
      .catch(() => setPolicies([]))
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  async function handleDelete(id) {
    await deletePolicy(id);
    load();
  }

  async function handleAddVm(policyId) {
    if (!nsId || !mciId) { alert('NS/MCI not set'); return; }
    await addVmToPolicy(policyId, { namespaceId: nsId, targetScope: 'mci', targetId: mciId });
    load();
  }

  return (
    <div className="space-y-4">
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b flex justify-between items-center">
          <span className="font-semibold text-sm">Trigger Policies</span>
          <button onClick={() => setShowCreate(!showCreate)} className="text-xs bg-red-600 text-white px-3 py-1 rounded hover:bg-red-700">
            {showCreate ? 'Cancel' : '+ New Policy'}
          </button>
        </div>
        {showCreate && <CreatePolicyForm onCreated={() => { setShowCreate(false); load(); }} />}
        <div className="p-4 overflow-auto">
          {loading ? <p className="text-sm text-gray-400">Loading...</p> : policies.length === 0 ? (
            <p className="text-sm text-gray-400">No policies</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                <th className="px-3 py-2 border-b text-xs text-gray-500">ID</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Title</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Resource</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Thresholds</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">VMs</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Actions</th>
              </tr></thead>
              <tbody>
                {policies.map(p => (
                  <tr key={p.id} className="hover:bg-gray-50">
                    <td className="px-3 py-2 border-b">{p.id}</td>
                    <td className="px-3 py-2 border-b">{p.title}</td>
                    <td className="px-3 py-2 border-b">{p.resourceType}</td>
                    <td className="px-3 py-2 border-b text-xs">
                      {p.thresholdCondition && `I:${p.thresholdCondition.info} W:${p.thresholdCondition.warning} C:${p.thresholdCondition.critical}`}
                    </td>
                    <td className="px-3 py-2 border-b text-xs">{p.vms?.length || 0}</td>
                    <td className="px-3 py-2 border-b text-right space-x-2">
                      <button onClick={() => handleAddVm(p.id)} className="text-xs text-blue-500 hover:text-blue-700">+MCI</button>
                      <button onClick={() => handleDelete(p.id)} className="text-xs text-red-500 hover:text-red-700">Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function CreatePolicyForm({ onCreated }) {
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [resource, setResource] = useState('CPU');
  const [agg, setAgg] = useState('AVG');
  const [info, setInfo] = useState(20);
  const [warning, setWarning] = useState(50);
  const [critical, setCritical] = useState(80);
  const [hold, setHold] = useState('5m');
  const [repeat, setRepeat] = useState('1h');

  async function submit(e) {
    e.preventDefault();
    await createPolicy({
      title, description: desc, resourceType: resource, aggregationType: agg,
      holdDuration: hold, repeatInterval: repeat,
      thresholdCondition: { info: +info, warning: +warning, critical: +critical },
    });
    onCreated();
  }

  return (
    <form onSubmit={submit} className="p-4 border-b bg-gray-50 space-y-3">
      <div className="grid grid-cols-2 gap-3">
        <input placeholder="Title" required value={title} onChange={e => setTitle(e.target.value)} className="border rounded px-3 py-1.5 text-sm" />
        <input placeholder="Description" value={desc} onChange={e => setDesc(e.target.value)} className="border rounded px-3 py-1.5 text-sm" />
        <select value={resource} onChange={e => setResource(e.target.value)} className="border rounded px-3 py-1.5 text-sm">
          {RESOURCE_TYPES.map(r => <option key={r}>{r}</option>)}
        </select>
        <select value={agg} onChange={e => setAgg(e.target.value)} className="border rounded px-3 py-1.5 text-sm">
          {AGG_TYPES.map(a => <option key={a}>{a}</option>)}
        </select>
      </div>
      <div className="grid grid-cols-3 gap-3">
        <input type="number" placeholder="Info" value={info} onChange={e => setInfo(e.target.value)} className="border rounded px-3 py-1.5 text-sm" />
        <input type="number" placeholder="Warning" value={warning} onChange={e => setWarning(e.target.value)} className="border rounded px-3 py-1.5 text-sm" />
        <input type="number" placeholder="Critical" value={critical} onChange={e => setCritical(e.target.value)} className="border rounded px-3 py-1.5 text-sm" />
      </div>
      <div className="grid grid-cols-2 gap-3">
        <input placeholder="Hold (e.g. 5m)" value={hold} onChange={e => setHold(e.target.value)} className="border rounded px-3 py-1.5 text-sm" />
        <input placeholder="Repeat (e.g. 1h)" value={repeat} onChange={e => setRepeat(e.target.value)} className="border rounded px-3 py-1.5 text-sm" />
      </div>
      <button type="submit" className="bg-red-600 text-white px-4 py-1.5 rounded text-sm hover:bg-red-700">Create</button>
    </form>
  );
}

function NotiHistoryTab() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getNotiHistory(1, 50).then(d => setHistory(d.content || []))
      .catch(() => setHistory([]))
      .finally(() => setLoading(false));
  }, []);

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-4 py-3 border-b font-semibold text-sm">Notification History</div>
      <div className="p-4 overflow-auto">
        {loading ? <p className="text-sm text-gray-400">Loading...</p> : history.length === 0 ? (
          <p className="text-sm text-gray-400">No notifications</p>
        ) : (
          <table className="w-full text-sm">
            <thead><tr className="bg-gray-50 text-left">
              <th className="px-3 py-2 border-b text-xs text-gray-500">ID</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Channel</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Recipients</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Status</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Time</th>
            </tr></thead>
            <tbody>
              {history.map(h => (
                <tr key={h.id} className="hover:bg-gray-50">
                  <td className="px-3 py-2 border-b">{h.id}</td>
                  <td className="px-3 py-2 border-b">{h.channel}</td>
                  <td className="px-3 py-2 border-b text-xs">{h.recipients?.join(', ')}</td>
                  <td className="px-3 py-2 border-b">
                    <span className={`text-xs px-2 py-0.5 rounded-full ${h.isSucceeded ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                      {h.isSucceeded ? 'OK' : 'FAIL'}
                    </span>
                  </td>
                  <td className="px-3 py-2 border-b text-xs">{h.createdAt}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
