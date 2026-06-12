import { useState, useEffect, Fragment } from 'react';
import {
  getPolicies, createPolicy, deletePolicy, updatePolicy,
  addNodeToPolicy, removeNodeFromPolicy, updatePolicyChannels,
  getNotiChannels, getNotiHistory, sendTestNotification,
} from '../api/trigger';
import { getInfraList } from '../api/tumblebug';
import { formatLocalTime } from '../utils/time';
import { useParams } from 'react-router-dom';

const TABS = ['Policies', 'Notification History'];
const RESOURCE_TYPES = ['CPU', 'MEMORY', 'DISK'];
const AGG_TYPES = ['AVG', 'MAX', 'MIN', 'LAST'];
// Backend stores resourceType as the measurement (cpu/mem/disk); map back to the enum name.
const RESOURCE_BY_MEASUREMENT = { cpu: 'CPU', mem: 'MEMORY', disk: 'DISK' };
const HISTORY_PAGE_SIZE = 20;

// Backend (/policy/{id}/channel and /noti/test) accepts only the short channel name:
// kakao, sms, email, slack, discord, teams. NotiChannel.name (e.g. "email_smtp.gmail.com")
// matches that short name in its part before the first '_'.
const channelShortName = (name) => (name || '').split('_')[0].toLowerCase();
const RECIPIENT_HINTS = {
  email: 'Email addresses (e.g. a@b.com, c@d.com)',
  slack: 'Channel name or ID (e.g. #alerts, C0123ABCD)',
  discord: 'Webhook URL (e.g. https://discord.com/api/webhooks/...)',
  teams: 'Webhook URL (Teams Workflows incoming webhook)',
  sms: 'Phone numbers (e.g. 01012345678, 01087654321)',
  kakao: 'Phone number (e.g. 01012345678)',
};
const cap = (s) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : s);
const nodeIdOf = (n) => n.node_id ?? n.id ?? n.name;
const nodeLabelOf = (n) => n.name ?? n.node_id ?? n.id;
const parseRecipients = (s) => (s || '').split(',').map(x => x.trim()).filter(Boolean);

// Strip the raw Java exception class names and stack trace, leaving only the
// human-readable failure message.
function cleanError(raw) {
  if (!raw) return 'No error detail recorded.';
  const out = [];
  for (const line of String(raw).split(/\r?\n/)) {
    if (/^\s*at\s+/.test(line)) continue;                 // stack frame
    if (/^\s*\.\.\.\s*\d+\s*more\s*$/.test(line)) continue; // "... N more"
    let l = line.replace(/^\s*Caused by:\s*/, '');         // keep the cause message
    // remove a leading fully-qualified exception class name (e.g. java.lang.RuntimeException: )
    l = l.replace(/^([\w$]+\.)+[\w$]*(Exception|Error|Throwable)(:\s*|\s*$)/, '');
    l = l.trim();
    if (l) out.push(l);
  }
  return out.join(' ').trim() || 'Delivery failed.';
}

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
  const [infras, setInfras] = useState([]);     // infras (each with embedded `node` list)
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
    if (!nsId) { setInfras([]); return; }
    // getInfraList returns each infra with its embedded `node` list, so VM selection
    // works even when the route has no infraId (namespace-level alert page).
    getInfraList(nsId).then(list => setInfras(list || [])).catch(() => setInfras([]));
  }, [nsId]);

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
            nsId={nsId} infraId={infraId} channels={channels} infras={infras}
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
                    key={p.id} p={p} nsId={nsId} infraId={infraId} channels={channels} infras={infras}
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

function PolicyRow({ p, nsId, infraId, channels, infras, expanded, onToggle, onChanged, onDelete }) {
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
            <ManagePanel p={p} nsId={nsId} infraId={infraId} channels={channels} infras={infras} onChanged={onChanged} />
          </td>
        </tr>
      )}
    </>
  );
}

function ManagePanel({ p, nsId, infraId, channels, infras, onChanged }) {
  const vms = p.vms || [];
  const [busy, setBusy] = useState(false);

  // Initialize channel editor from the policy's current channels.
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
    setBusy(true);
    try {
      await updatePolicyChannels(p.id, buildChannelPayload(chanSel));
      onChanged();
      alert('Channels updated');
    } catch (e) { alert('Channel update failed: ' + (e?.message || e)); }
    finally { setBusy(false); }
  }

  return (
    <div className="space-y-4">
      <PolicySettingsEditor p={p} onChanged={onChanged} />

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
        <TargetPicker nsId={nsId} defaultInfraId={infraId} infras={infras} busy={busy} onAdd={addTargets} addLabel="Add targets" />
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

// Edit an existing policy's settings (title, thresholds, resource, aggregation, hold/repeat).
function PolicySettingsEditor({ p, onChanged }) {
  const [open, setOpen] = useState(false);
  const [title, setTitle] = useState(p.title || '');
  const [desc, setDesc] = useState(p.description || '');
  const [resource, setResource] = useState(RESOURCE_BY_MEASUREMENT[(p.resourceType || '').toLowerCase()] || 'CPU');
  const [agg, setAgg] = useState((p.aggregationType || 'AVG').toUpperCase());
  const [info, setInfo] = useState(p.thresholdCondition?.info ?? 0);
  const [warning, setWarning] = useState(p.thresholdCondition?.warning ?? 0);
  const [critical, setCritical] = useState(p.thresholdCondition?.critical ?? 0);
  const [hold, setHold] = useState(p.holdDuration || '5m');
  const [repeat, setRepeat] = useState(p.repeatInterval || '1h');
  const [busy, setBusy] = useState(false);

  async function save() {
    setBusy(true);
    try {
      await updatePolicy(p.id, {
        title, description: desc, resourceType: resource, aggregationType: agg,
        holdDuration: hold, repeatInterval: repeat,
        thresholdCondition: { info: +info, warning: +warning, critical: +critical },
      });
      onChanged();
      alert('Policy updated');
    } catch (e) { alert('Update failed: ' + (e?.response?.data?.message || e?.message || e)); }
    finally { setBusy(false); }
  }

  return (
    <div>
      <button type="button" onClick={() => setOpen(o => !o)}
        className="text-xs font-semibold text-gray-600 hover:text-gray-800">
        {open ? '▾' : '▸'} Policy settings (title, thresholds, resource, hold/repeat)
      </button>
      {open && (
        <div className="mt-2 space-y-3 bg-white border rounded p-3">
          <Field label="Title" hint="Unique name to identify this policy">
            <input value={title} onChange={e => setTitle(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
          </Field>
          <Field label="Description" hint="Optional note about this policy">
            <input value={desc} onChange={e => setDesc(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
          </Field>
          <Field label="Resource" hint="Metric type to evaluate against the thresholds">
            <select value={resource} onChange={e => setResource(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm">
              {RESOURCE_TYPES.map(r => <option key={r}>{r}</option>)}
            </select>
          </Field>
          <Field label="Aggregation" hint="How to evaluate values over the interval (avg / max / min / last)">
            <select value={agg} onChange={e => setAgg(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm">
              {AGG_TYPES.map(a => <option key={a}>{a}</option>)}
            </select>
          </Field>
          <div className="border-t pt-3 space-y-3">
            <div className="text-xs font-semibold text-gray-600">Thresholds (%) — per-level alert criteria</div>
            <Field label="Info" hint="Lowest alert level">
              <input type="number" value={info} onChange={e => setInfo(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
            </Field>
            <Field label="Warning" hint="Middle alert level">
              <input type="number" value={warning} onChange={e => setWarning(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
            </Field>
            <Field label="Critical" hint="Highest alert level">
              <input type="number" value={critical} onChange={e => setCritical(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
            </Field>
          </div>
          <div className="border-t pt-3 space-y-3">
            <Field label="Hold Duration" hint="Exceed threshold continuously this long before firing (e.g. 5m, 0s)">
              <input value={hold} onChange={e => setHold(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
            </Field>
            <Field label="Repeat Interval" hint="How often to re-notify while firing (e.g. 1h)">
              <input value={repeat} onChange={e => setRepeat(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
            </Field>
          </div>
          <button disabled={busy} onClick={save}
            className="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 disabled:opacity-50">
            Save settings
          </button>
        </div>
      )}
    </div>
  );
}

function CreatePolicyForm({ nsId, infraId, channels, infras, onCreated }) {
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
    <form onSubmit={submit} className="p-4 border-b bg-gray-50 space-y-3">
      <Field label="Title" hint="Unique name to identify this policy">
        <input required value={title} onChange={e => setTitle(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
      </Field>
      <Field label="Description" hint="Optional note about this policy">
        <input value={desc} onChange={e => setDesc(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
      </Field>
      <Field label="Resource" hint="Metric type to evaluate against the thresholds">
        <select value={resource} onChange={e => setResource(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm">
          {RESOURCE_TYPES.map(r => <option key={r}>{r}</option>)}
        </select>
      </Field>
      <Field label="Aggregation" hint="How to evaluate values over the interval (avg / max / min / last)">
        <select value={agg} onChange={e => setAgg(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm">
          {AGG_TYPES.map(a => <option key={a}>{a}</option>)}
        </select>
      </Field>

      <div className="border-t pt-3 space-y-3">
        <div className="text-xs font-semibold text-gray-600">Thresholds (%) — per-level alert criteria</div>
        <Field label="Info" hint="Lowest alert level">
          <input type="number" value={info} onChange={e => setInfo(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
        </Field>
        <Field label="Warning" hint="Middle alert level">
          <input type="number" value={warning} onChange={e => setWarning(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
        </Field>
        <Field label="Critical" hint="Highest alert level">
          <input type="number" value={critical} onChange={e => setCritical(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
        </Field>
      </div>

      <div className="border-t pt-3 space-y-3">
        <Field label="Hold Duration" hint="Exceed threshold continuously this long before firing (e.g. 5m, 0s)">
          <input value={hold} onChange={e => setHold(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
        </Field>
        <Field label="Repeat Interval" hint="How often to re-notify while firing (e.g. 1h)">
          <input value={repeat} onChange={e => setRepeat(e.target.value)} className="w-full border rounded px-3 py-1.5 text-sm" />
        </Field>
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
        <TargetPicker nsId={nsId} defaultInfraId={infraId} infras={infras} onAdd={addPending} addLabel="Add" />
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

// Label shown to the LEFT of the input (with a small description underneath).
function Field({ label, hint, children }) {
  return (
    <div className="flex items-start gap-3">
      <div className="w-48 shrink-0 pt-1.5">
        <div className="text-xs font-medium text-gray-700">{label}</div>
        {hint && <div className="text-[11px] text-gray-400 leading-tight">{hint}</div>}
      </div>
      <div className="flex-1 min-w-0">{children}</div>
    </div>
  );
}

// Channel selection + per-channel recipients + per-channel Test button.
// value = { [shortName]: { checked, recipients } }
function ChannelPicker({ channels, value, onChange }) {
  const [testing, setTesting] = useState('');

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

  async function test(short) {
    const recipients = parseRecipients((value[short] || {}).recipients);
    if (!recipients.length) { alert('Enter at least one recipient first.'); return; }
    setTesting(short);
    try {
      await sendTestNotification({
        channelName: short,
        recipients,
        title: 'Test notification',
        message: 'This is a test alert from mc-observability.',
      });
      alert(`Test sent to ${short}. Check the Notification History tab for the delivery result.`);
    } catch (e) {
      alert('Test send failed: ' + (e?.message || e));
    } finally {
      setTesting('');
    }
  }

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
            <button
              type="button"
              disabled={!sel.checked || testing === short}
              onClick={() => test(short)}
              className="text-xs border border-blue-300 text-blue-600 px-2 py-1 rounded hover:bg-blue-50 disabled:opacity-40 shrink-0"
              title="Send a test notification via RabbitMQ to this channel"
            >
              {testing === short ? 'Sending...' : 'Test'}
            </button>
          </div>
        );
      })}
      <p className="text-[11px] text-gray-400">Comma-separate multiple recipients. Checked channels need at least one recipient. Test delivers a real message through RabbitMQ.</p>
    </div>
  );
}

// Pick an infra, then its VMs. Infra level = multi-select VMs; Node level = a single VM.
// Both are added as `node` targets. Works at namespace-level (route has no infraId) by
// letting the user choose the infra from the dropdown.
function TargetPicker({ nsId, defaultInfraId, infras, busy, onAdd, addLabel }) {
  const matchInfra = (i) => i.id === defaultInfraId || i.name === defaultInfraId;
  const [selInfra, setSelInfra] = useState(() => {
    const m = (infras || []).find(matchInfra);
    return m ? (m.id ?? m.name) : (infras?.[0]?.id ?? infras?.[0]?.name ?? '');
  });
  const [level, setLevel] = useState('infra');
  const [checked, setChecked] = useState({}); // nodeId -> bool (infra level)
  const [single, setSingle] = useState('');   // node level

  if (!nsId) {
    return <p className="text-xs text-gray-400">Select a namespace to choose target VMs.</p>;
  }
  if (!infras || infras.length === 0) {
    return <p className="text-xs text-gray-400">No infras found in this namespace.</p>;
  }

  const infra = infras.find(i => String(i.id ?? i.name) === String(selInfra)) || infras[0];
  const nodes = infra?.node || [];

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
      <div className="flex items-center gap-2 text-xs">
        <span className="text-gray-500">Infra</span>
        <select
          value={selInfra}
          onChange={e => { setSelInfra(e.target.value); setChecked({}); setSingle(''); }}
          className="border rounded px-2 py-1 text-xs">
          {infras.map(i => {
            const id = i.id ?? i.name;
            return <option key={id} value={id}>{i.name ?? id}</option>;
          })}
        </select>
      </div>

      <div className="flex items-center gap-4 text-xs">
        <label className="flex items-center gap-1 cursor-pointer">
          <input type="radio" name={`level-${selInfra}`} checked={level === 'infra'} onChange={() => setLevel('infra')} /> Infra (select VMs)
        </label>
        <label className="flex items-center gap-1 cursor-pointer">
          <input type="radio" name={`level-${selInfra}`} checked={level === 'node'} onChange={() => setLevel('node')} /> Node (single VM)
        </label>
      </div>

      {nodes.length === 0 ? (
        <p className="text-xs text-gray-400">No VMs found in this infra.</p>
      ) : level === 'infra' ? (
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

      <button type="button" disabled={busy || nodes.length === 0} onClick={commit} className="bg-blue-600 text-white px-3 py-1 rounded text-xs hover:bg-blue-700 disabled:opacity-50">
        {addLabel || 'Add'}
      </button>
    </div>
  );
}

// chanSel({ [short]: { checked, recipients } }) -> [{ channelName, recipients[] }]
function buildChannelPayload(chanSel) {
  return Object.entries(chanSel)
    .filter(([, v]) => v && v.checked)
    .map(([short, v]) => ({ channelName: short, recipients: parseRecipients(v.recipients) }))
    .filter(c => c.recipients.length > 0);
}

function NotiHistoryTab() {
  const [page, setPage] = useState(1);
  const [data, setData] = useState({ content: [], totalPages: 1, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [open, setOpen] = useState(null);

  useEffect(() => {
    setLoading(true);
    getNotiHistory(page, HISTORY_PAGE_SIZE)
      .then(d => setData({ content: d.content || [], totalPages: d.totalPages || 1, totalElements: d.totalElements || 0 }))
      .catch(() => setData({ content: [], totalPages: 1, totalElements: 0 }))
      .finally(() => setLoading(false));
  }, [page]);

  const { content, totalPages, totalElements } = data;

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-4 py-3 border-b font-semibold text-sm flex justify-between items-center">
        <span>Notification History</span>
        {totalElements > 0 && <span className="text-xs font-normal text-gray-400">{totalElements} total</span>}
      </div>
      <div className="p-4 overflow-auto">
        {loading ? <p className="text-sm text-gray-400">Loading...</p> : content.length === 0 ? (
          <p className="text-sm text-gray-400">No notifications</p>
        ) : (
          <>
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
                {content.map(h => {
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
                        <td className="px-3 py-2 border-b text-xs">{formatLocalTime(h.createdAt)}</td>
                      </tr>
                      {isOpen && (
                        <tr>
                          <td colSpan={6} className="border-b bg-gray-50 px-4 py-3">
                            <div className="space-y-2 text-xs">
                              <div><span className="text-gray-500">Channel:</span> {h.channel}</div>
                              <div><span className="text-gray-500">Recipients:</span> {h.recipients?.join(', ') || '-'}</div>
                              <div><span className="text-gray-500">Time:</span> {formatLocalTime(h.createdAt)}</div>
                              {h.isSucceeded ? (
                                <div className="text-green-700">Delivered successfully.</div>
                              ) : (
                                <div>
                                  <div className="text-red-700 font-semibold mb-1">Failure cause</div>
                                  <div className="whitespace-pre-wrap break-words bg-red-50 border border-red-200 rounded p-2 text-[12px] text-red-800">
                                    {cleanError(h.exception)}
                                  </div>
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

            <div className="flex items-center justify-between mt-3 text-xs text-gray-500">
              <button
                disabled={page <= 1}
                onClick={() => { setOpen(null); setPage(p => Math.max(1, p - 1)); }}
                className="px-3 py-1 border rounded disabled:opacity-40 hover:bg-gray-50">
                ‹ Prev
              </button>
              <span>Page {page} / {totalPages || 1}</span>
              <button
                disabled={page >= totalPages}
                onClick={() => { setOpen(null); setPage(p => p + 1); }}
                className="px-3 py-1 border rounded disabled:opacity-40 hover:bg-gray-50">
                Next ›
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
