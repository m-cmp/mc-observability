import { useState, useEffect, Fragment } from 'react';
import {
  getPolicies, createPolicy, deletePolicy,
  addNodeToPolicy, removeNodeFromPolicy, updatePolicyChannels,
  getNotiChannels, getNotiHistory,
} from '../api/trigger';
import { getNodeList } from '../api/node';
import { useParams } from 'react-router-dom';

const TABS = ['Policies', 'Notification History'];
const RESOURCE_TYPES = ['CPU', 'MEMORY', 'DISK'];
const AGG_TYPES = ['AVG', 'MAX', 'MIN', 'LAST'];

// Backend(/policy/{id}/channel)는 짧은 채널명만 허용: kakao, sms, email, slack, discord, teams.
// NotiChannel.name("email_smtp.gmail.com" 등)에서 '_' 앞부분이 짧은 이름과 일치한다.
const channelShortName = (name) => (name || '').split('_')[0].toLowerCase();
const RECIPIENT_HINTS = {
  email: 'a@b.com, c@d.com',
  slack: '#alerts 또는 webhook URL',
  discord: 'webhook URL',
  teams: 'webhook URL',
  sms: '01012345678, 01087654321',
  kakao: '01012345678',
};
const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);
const nodeIdOf = (n) => n.node_id ?? n.id ?? n.name;
const nodeLabelOf = (n) => n.name ?? n.node_id ?? n.id;

export default function AlertManager() {
  const { nsId, infraId } = useParams();
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
      {tab === 0 && <PoliciesTab nsId={nsId} infraId={infraId} />}
      {tab === 1 && <NotiHistoryTab />}
    </div>
  );
}

function PoliciesTab({ nsId, infraId }) {
  const [policies, setPolicies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showCreate, setShowCreate] = useState(false);
  const [channels, setChannels] = useState([]); // available noti channels
  const [nodes, setNodes] = useState([]);       // VMs in this infra
  const [expanded, setExpanded] = useState(null);

  const load = () => {
    setLoading(true);
    getPolicies(1, 50).then(data => setPolicies(data.content || []))
      .catch(() => setPolicies([]))
      .finally(() => setLoading(false));
  };
  useEffect(load, []);

  useEffect(() => {
    getNotiChannels().then(d => setChannels((d.notiChannels || []).filter(c => c.isActive !== false))).catch(() => setChannels([]));
  }, []);

  useEffect(() => {
    if (!nsId || !infraId) { setNodes([]); return; }
    getNodeList(nsId, infraId).then(list => setNodes(list || [])).catch(() => setNodes([]));
  }, [nsId, infraId]);

  async function handleDelete(id) {
    if (!confirm('Delete this policy?')) return;
    await deletePolicy(id);
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
        {showCreate && (
          <CreatePolicyForm
            nsId={nsId} infraId={infraId} channels={channels} nodes={nodes}
            onCreated={() => { setShowCreate(false); load(); }}
          />
        )}
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
                <th className="px-3 py-2 border-b text-xs text-gray-500">Targets</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Channels</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Actions</th>
              </tr></thead>
              <tbody>
                {policies.map(p => (
                  <PolicyRow
                    key={p.id} p={p} nsId={nsId} infraId={infraId} channels={channels} nodes={nodes}
                    expanded={expanded === p.id} onToggle={() => setExpanded(expanded === p.id ? null : p.id)}
                    onChanged={load} onDelete={() => handleDelete(p.id)}
                  />
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

function PolicyRow({ p, nsId, infraId, channels, nodes, expanded, onToggle, onChanged, onDelete }) {
  const vms = p.vms || [];
  const chans = p.notiChannels || [];
  return (
    <>
      <tr className="hover:bg-gray-50">
        <td className="px-3 py-2 border-b">{p.id}</td>
        <td className="px-3 py-2 border-b">{p.title}</td>
        <td className="px-3 py-2 border-b">{p.resourceType}</td>
        <td className="px-3 py-2 border-b text-xs">
          {p.thresholdCondition && `I:${p.thresholdCondition.info} W:${p.thresholdCondition.warning} C:${p.thresholdCondition.critical}`}
        </td>
        <td className="px-3 py-2 border-b text-xs">{vms.length}</td>
        <td className="px-3 py-2 border-b text-xs">
          {chans.length ? chans.map(c => channelShortName(c.name)).join(', ') : <span className="text-gray-400">none</span>}
        </td>
        <td className="px-3 py-2 border-b text-right space-x-2 whitespace-nowrap">
          <button onClick={onToggle} className="text-xs text-blue-500 hover:text-blue-700">{expanded ? 'Close' : 'Manage'}</button>
          <button onClick={onDelete} className="text-xs text-red-500 hover:text-red-700">Delete</button>
        </td>
      </tr>
      {expanded && (
        <tr>
          <td colSpan={7} className="border-b bg-gray-50 px-4 py-3">
            <ManagePanel p={p} nsId={nsId} infraId={infraId} channels={channels} nodes={nodes} onChanged={onChanged} />
          </td>
        </tr>
      )}
    </>
  );
}

function ManagePanel({ p, nsId, infraId, channels, nodes, onChanged }) {
  const vms = p.vms || [];
  const [busy, setBusy] = useState(false);

  // 채널 편집: 기존 설정으로 초기화
  const [chanSel, setChanSel] = useState(() => {
    const init = {};
    (p.notiChannels || []).forEach(c => {
      init[channelShortName(c.name)] = { checked: true, recipients: (c.recipients || []).join(', ') };
    });
    return init;
  });

  async function removeTarget(vm) {
    setBusy(true);
    try {
      await removeNodeFromPolicy(p.id, { namespaceId: vm.namespaceId, targetScope: vm.targetScope, targetId: vm.targetId });
      onChanged();
    } catch (e) { alert('Remove failed: ' + (e?.message || e)); }
    finally { setBusy(false); }
  }

  async function addTargets(targets) {
    if (!targets.length) return;
    setBusy(true);
    try {
      for (const t of targets) {
        await addNodeToPolicy(p.id, { namespaceId: nsId, targetScope: t.targetScope, targetId: t.targetId });
      }
      onChanged();
    } catch (e) { alert('Add target failed: ' + (e?.message || e)); }
    finally { setBusy(false); }
  }

  async function saveChannels() {
    const payload = buildChannelPayload(chanSel);
    setBusy(true);
    try {
      await updatePolicyChannels(p.id, payload);
      onChanged();
      alert('Channels updated');
    } catch (e) { alert('Channel update failed: ' + (e?.message || e)); }
    finally { setBusy(false); }
  }

  return (
    <div className="space-y-4">
      <div>
        <div className="text-xs font-semibold text-gray-600 mb-1">Alarm targets</div>
        {vms.length === 0 ? <p className="text-xs text-gray-400 mb-2">No targets</p> : (
          <div className="flex flex-wrap gap-2 mb-2">
            {vms.map(vm => (
              <span key={`${vm.targetScope}-${vm.targetId}`} className="inline-flex items-center gap-1 text-xs bg-white border rounded px-2 py-1">
                <span className="text-gray-400">{vm.targetScope}</span> {vm.targetId}
                <button disabled={busy} onClick={() => removeTarget(vm)} className="text-red-500 hover:text-red-700 ml-1">×</button>
              </span>
            ))}
          </div>
        )}
        <TargetPicker nsId={nsId} infraId={infraId} nodes={nodes} busy={busy} onAdd={addTargets} addLabel="Add targets" />
      </div>

      <div>
        <div className="text-xs font-semibold text-gray-600 mb-1">Notification channels</div>
        <ChannelPicker channels={channels} value={chanSel} onChange={setChanSel} />
        <button disabled={busy} onClick={saveChannels} className="mt-2 bg-gray-700 text-white px-3 py-1 rounded text-xs hover:bg-gray-800 disabled:opacity-50">
          Save channels
        </button>
      </div>
    </div>
  );
}

function CreatePolicyForm({ nsId, infraId, channels, nodes, onCreated }) {
  const [title, setTitle] = useState('');
  const [desc, setDesc] = useState('');
  const [resource, setResource] = useState('CPU');
  const [agg, setAgg] = useState('AVG');
  const [info, setInfo] = useState(20);
  const [warning, setWarning] = useState(50);
  const [critical, setCritical] = useState(80);
  const [hold, setHold] = useState('5m');
  const [repeat, setRepeat] = useState('1h');
  const [chanSel, setChanSel] = useState({});
  const [pendingTargets, setPendingTargets] = useState([]); // [{targetScope, targetId, label}]
  const [busy, setBusy] = useState(false);

  function addPending(targets) {
    setPendingTargets(prev => {
      const map = new Map(prev.map(t => [`${t.targetScope}-${t.targetId}`, t]));
      targets.forEach(t => map.set(`${t.targetScope}-${t.targetId}`, t));
      return [...map.values()];
    });
  }

  async function submit(e) {
    e.preventDefault();
    setBusy(true);
    try {
      const created = await createPolicy({
        title, description: desc, resourceType: resource, aggregationType: agg,
        holdDuration: hold, repeatInterval: repeat,
        thresholdCondition: { info: +info, warning: +warning, critical: +critical },
      });
      const policyId = created?.id ?? created?.data?.id;
      if (!policyId) throw new Error('No policy id returned');

      const channelPayload = buildChannelPayload(chanSel);
      if (channelPayload.length) await updatePolicyChannels(policyId, channelPayload);

      for (const t of pendingTargets) {
        await addNodeToPolicy(policyId, { namespaceId: nsId, targetScope: t.targetScope, targetId: t.targetId });
      }
      onCreated();
    } catch (err) {
      alert('Create failed: ' + (err?.message || err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <form onSubmit={submit} className="p-4 border-b bg-gray-50 space-y-4">
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

      {/* Alarm targets */}
      <div className="border-t pt-3">
        <div className="text-xs font-semibold text-gray-600 mb-2">Alarm targets</div>
        {pendingTargets.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-2">
            {pendingTargets.map(t => (
              <span key={`${t.targetScope}-${t.targetId}`} className="inline-flex items-center gap-1 text-xs bg-white border rounded px-2 py-1">
                <span className="text-gray-400">{t.targetScope}</span> {t.label || t.targetId}
                <button type="button" onClick={() => setPendingTargets(prev => prev.filter(x => !(x.targetScope === t.targetScope && x.targetId === t.targetId)))}
                  className="text-red-500 hover:text-red-700 ml-1">×</button>
              </span>
            ))}
          </div>
        )}
        <TargetPicker nsId={nsId} infraId={infraId} nodes={nodes} onAdd={addPending} addLabel="Add" />
      </div>

      {/* Notification channels */}
      <div className="border-t pt-3">
        <div className="text-xs font-semibold text-gray-600 mb-2">Notification channels</div>
        <ChannelPicker channels={channels} value={chanSel} onChange={setChanSel} />
      </div>

      <button type="submit" disabled={busy} className="bg-red-600 text-white px-4 py-1.5 rounded text-sm hover:bg-red-700 disabled:opacity-50">
        {busy ? 'Creating...' : 'Create'}
      </button>
    </form>
  );
}

// 채널 선택 + 채널별 수신자 입력. value = { [shortName]: { checked, recipients } }
function ChannelPicker({ channels, value, onChange }) {
  if (!channels || channels.length === 0) {
    return <p className="text-xs text-gray-400">No notification channels configured on the server.</p>;
  }
  const toggle = (short) => {
    const cur = value[short] || { checked: false, recipients: '' };
    onChange({ ...value, [short]: { ...cur, checked: !cur.checked } });
  };
  const setRecipients = (short, recipients) => {
    const cur = value[short] || { checked: true, recipients: '' };
    onChange({ ...value, [short]: { ...cur, recipients, checked: true } });
  };
  return (
    <div className="space-y-2">
      {channels.map(c => {
        const short = channelShortName(c.name);
        const sel = value[short] || { checked: false, recipients: '' };
        return (
          <div key={c.id ?? c.name} className="flex items-center gap-2">
            <label className="flex items-center gap-1.5 text-xs w-28 shrink-0 cursor-pointer">
              <input type="checkbox" checked={!!sel.checked} onChange={() => toggle(short)} />
              {cap(short)}
            </label>
            <input
              disabled={!sel.checked}
              placeholder={RECIPIENT_HINTS[short] || 'recipients (comma-separated)'}
              value={sel.recipients}
              onChange={e => setRecipients(short, e.target.value)}
              className="flex-1 border rounded px-2 py-1 text-xs disabled:bg-gray-100"
            />
          </div>
        );
      })}
      <p className="text-[11px] text-gray-400">Comma-separate multiple recipients. Checked channels need at least one recipient.</p>
    </div>
  );
}

// Infra 레벨 = 인프라 내 VM 다중선택, Node 레벨 = 단일 VM. 둘 다 node 타깃으로 추가.
function TargetPicker({ nsId, infraId, nodes, busy, onAdd, addLabel }) {
  const [level, setLevel] = useState('infra');
  const [checked, setChecked] = useState({}); // nodeId -> bool (infra level)
  const [single, setSingle] = useState('');   // node level

  if (!nsId || !infraId) {
    return <p className="text-xs text-gray-400">Open the alert page from a specific infra to choose target VMs.</p>;
  }
  if (!nodes || nodes.length === 0) {
    return <p className="text-xs text-gray-400">No VMs found in this infra.</p>;
  }

  const toggle = (id) => setChecked(prev => ({ ...prev, [id]: !prev[id] }));

  const commit = () => {
    let targets = [];
    if (level === 'infra') {
      targets = nodes.filter(n => checked[nodeIdOf(n)]).map(n => ({ targetScope: 'node', targetId: nodeIdOf(n), label: nodeLabelOf(n) }));
    } else if (single) {
      const n = nodes.find(x => String(nodeIdOf(x)) === String(single));
      if (n) targets = [{ targetScope: 'node', targetId: nodeIdOf(n), label: nodeLabelOf(n) }];
    }
    if (!targets.length) { alert('Select at least one VM.'); return; }
    onAdd(targets);
    setChecked({}); setSingle('');
  };

  return (
    <div className="space-y-2">
      <div className="flex items-center gap-4 text-xs">
        <label className="flex items-center gap-1 cursor-pointer">
          <input type="radio" name={`level-${infraId}`} checked={level === 'infra'} onChange={() => setLevel('infra')} /> Infra (select VMs)
        </label>
        <label className="flex items-center gap-1 cursor-pointer">
          <input type="radio" name={`level-${infraId}`} checked={level === 'node'} onChange={() => setLevel('node')} /> Node (single VM)
        </label>
      </div>

      {level === 'infra' ? (
        <div className="flex flex-wrap gap-x-4 gap-y-1">
          {nodes.map(n => {
            const id = nodeIdOf(n);
            return (
              <label key={id} className="flex items-center gap-1.5 text-xs cursor-pointer">
                <input type="checkbox" checked={!!checked[id]} onChange={() => toggle(id)} />
                {nodeLabelOf(n)} <span className="text-gray-400">({id})</span>
              </label>
            );
          })}
        </div>
      ) : (
        <select value={single} onChange={e => setSingle(e.target.value)} className="border rounded px-2 py-1 text-xs">
          <option value="">Select VM...</option>
          {nodes.map(n => {
            const id = nodeIdOf(n);
            return <option key={id} value={id}>{nodeLabelOf(n)} ({id})</option>;
          })}
        </select>
      )}

      <button type="button" disabled={busy} onClick={commit} className="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 disabled:opacity-50">
        {addLabel || 'Add'}
      </button>
    </div>
  );
}

// chanSel({ [short]: { checked, recipients } }) -> [{ channelName, recipients[] }]
function buildChannelPayload(chanSel) {
  return Object.entries(chanSel)
    .filter(([, v]) => v && v.checked)
    .map(([short, v]) => ({
      channelName: short,
      recipients: (v.recipients || '').split(',').map(s => s.trim()).filter(Boolean),
    }))
    .filter(c => c.recipients.length > 0);
}

function NotiHistoryTab() {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(null);

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
              <th className="px-3 py-2 border-b text-xs text-gray-500 w-6"></th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">ID</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Channel</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Recipients</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Status</th>
              <th className="px-3 py-2 border-b text-xs text-gray-500">Time</th>
            </tr></thead>
            <tbody>
              {history.map(h => {
                const isOpen = open === h.id;
                return (
                  <Fragment key={h.id}>
                    <tr className="hover:bg-gray-50 cursor-pointer" onClick={() => setOpen(isOpen ? null : h.id)}>
                      <td className="px-3 py-2 border-b text-gray-400 text-xs">{isOpen ? '▼' : '▶'}</td>
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
                    {isOpen && (
                      <tr>
                        <td colSpan={6} className="border-b bg-gray-50 px-4 py-3">
                          <div className="space-y-2 text-xs">
                            <div><span className="text-gray-500">Channel:</span> {h.channel}</div>
                            <div><span className="text-gray-500">Recipients:</span> {h.recipients?.join(', ') || '-'}</div>
                            <div><span className="text-gray-500">Time:</span> {h.createdAt}</div>
                            {h.isSucceeded ? (
                              <div className="text-green-700">Delivered successfully.</div>
                            ) : (
                              <div>
                                <div className="text-red-700 font-semibold mb-1">Failure cause</div>
                                <pre className="whitespace-pre-wrap break-all bg-red-50 border border-red-200 rounded p-2 text-[11px] text-red-800 max-h-64 overflow-auto">
{h.exception || 'No error detail recorded.'}
                                </pre>
                              </div>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </Fragment>
                );
              })}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
