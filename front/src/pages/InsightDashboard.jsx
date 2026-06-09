import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getAnomalySettings, createAnomalySetting, deleteAnomalySetting, getAnomalyHistory, getAnomalyMeasurements } from '../api/insight';
import { getPredictionHistory, runPrediction } from '../api/insight';
import MetricChart from '../components/MetricChart';

const TABS = ['Anomaly Detection', 'Prediction'];

export default function InsightDashboard() {
  const { nsId, infraId, nodeId } = useParams();
  const [tab, setTab] = useState(0);

  return (
    <div className="space-y-4">
      {/* Tab bar */}
      <div className="flex gap-1 bg-white rounded-lg shadow px-2 py-1">
        {TABS.map((t, i) => (
          <button key={t} onClick={() => setTab(i)}
            className={`px-4 py-2 text-sm rounded ${tab === i ? 'bg-purple-600 text-white' : 'text-gray-600 hover:bg-gray-100'}`}>
            {t}
          </button>
        ))}
      </div>
      {tab === 0 && <AnomalyTab nsId={nsId} infraId={infraId} nodeId={nodeId} />}
      {tab === 1 && <PredictionTab nsId={nsId} infraId={infraId} nodeId={nodeId} />}
    </div>
  );
}

function AnomalyTab({ nsId, infraId, nodeId }) {
  const [settings, setSettings] = useState([]);
  const [measurements, setMeasurements] = useState([]);
  const [selectedMeasurement, setSelectedMeasurement] = useState('');
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getAnomalySettings().then(setSettings).catch(() => {});
    getAnomalyMeasurements().then((d) => {
      if (Array.isArray(d)) setMeasurements(d.map(m => m.measurement || m));
      else setMeasurements([]);
    }).catch(() => {});
  }, []);

  async function loadHistory() {
    if (!selectedMeasurement) return;
    setLoading(true);
    try {
      const data = await getAnomalyHistory(nsId, infraId, nodeId, selectedMeasurement);
      setHistory(data.values || []);
    } catch { setHistory([]); }
    setLoading(false);
  }

  async function handleDelete(seq) {
    await deleteAnomalySetting(seq);
    setSettings(await getAnomalySettings());
  }

  const chartSeries = history.length > 0 ? [{
    name: 'Anomaly Score',
    data: history.map(h => ({ x: new Date(h.timestamp).getTime(), y: h.anomaly_score })),
  }] : [];

  return (
    <div className="space-y-4">
      {/* Settings table */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Anomaly Detection Settings</div>
        <div className="p-4 overflow-auto">
          {settings.length === 0 ? (
            <p className="text-sm text-gray-400">No settings configured</p>
          ) : (
            <table className="w-full text-sm">
              <thead><tr className="bg-gray-50 text-left">
                <th className="px-3 py-2 border-b text-xs text-gray-500">Seq</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">NS / Infra / Node</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500">Measurement</th>
                <th className="px-3 py-2 border-b text-xs text-gray-500 text-right">Actions</th>
              </tr></thead>
              <tbody>
                {settings.map((s, i) => (
                  <tr key={s.settingSeq || s.seq || i} className="hover:bg-gray-50">
                    <td className="px-3 py-2 border-b">{s.settingSeq || s.seq}</td>
                    <td className="px-3 py-2 border-b">{s.nsId || s.ns_id}/{s.infraId || s.infra_id}/{s.nodeId || s.node_id || '-'}</td>
                    <td className="px-3 py-2 border-b">{s.measurement || '-'}</td>
                    <td className="px-3 py-2 border-b text-right">
                      <button onClick={() => handleDelete(s.settingSeq || s.seq)} className="text-xs text-red-500 hover:text-red-700">Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* History chart */}
      <div className="bg-white rounded-lg shadow">
        <div className="px-4 py-3 border-b font-semibold text-sm">Anomaly Detection History</div>
        <div className="p-4">
          <div className="flex gap-3 mb-4">
            <select className="border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedMeasurement} onChange={(e) => setSelectedMeasurement(e.target.value)}>
              <option value="">Select Measurement</option>
              {measurements.map(m => <option key={m} value={m}>{m}</option>)}
            </select>
            <button onClick={loadHistory} disabled={loading} className="px-4 py-1.5 bg-purple-600 text-white rounded text-sm hover:bg-purple-700 disabled:opacity-50">
              {loading ? 'Loading...' : 'Load History'}
            </button>
          </div>
          <MetricChart title="Anomaly Score" series={chartSeries} height={240} chartType="line" />
        </div>
      </div>
    </div>
  );
}

function PredictionTab({ nsId, infraId, nodeId }) {
  const [measurements, setMeasurements] = useState([]);
  const [selectedMeasurement, setSelectedMeasurement] = useState('');
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getAnomalyMeasurements().then((d) => {
      if (Array.isArray(d)) setMeasurements(d.map(m => m.measurement || m));
      else setMeasurements([]);
    }).catch(() => {});
  }, []);

  async function loadHistory() {
    if (!selectedMeasurement) return;
    setLoading(true);
    try {
      const data = await getPredictionHistory(nsId, infraId, nodeId, selectedMeasurement);
      setHistory(data.values || []);
    } catch { setHistory([]); }
    setLoading(false);
  }

  async function handleRunPrediction() {
    if (!selectedMeasurement) return;
    setLoading(true);
    try {
      await runPrediction(nsId, infraId, nodeId, { measurement: selectedMeasurement });
      await loadHistory();
    } catch (e) {
      console.error('Prediction failed', e);
    }
    setLoading(false);
  }

  const chartSeries = history.length > 0 ? [{
    name: 'Prediction',
    data: history.map(h => ({ x: new Date(h.timestamp).getTime(), y: parseFloat(h.value) })),
  }] : [];

  return (
    <div className="bg-white rounded-lg shadow">
      <div className="px-4 py-3 border-b font-semibold text-sm">Prediction</div>
      <div className="p-4">
        <div className="flex gap-3 mb-4">
          <select className="border border-gray-300 rounded px-3 py-1.5 text-sm" value={selectedMeasurement} onChange={(e) => setSelectedMeasurement(e.target.value)}>
            <option value="">Select Measurement</option>
            {measurements.map(m => <option key={m} value={m}>{m}</option>)}
          </select>
          <button onClick={handleRunPrediction} disabled={loading} className="px-4 py-1.5 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 disabled:opacity-50">
            Run Prediction
          </button>
          <button onClick={loadHistory} disabled={loading} className="px-4 py-1.5 bg-purple-600 text-white rounded text-sm hover:bg-purple-700 disabled:opacity-50">
            Load History
          </button>
        </div>
        <MetricChart title="Prediction" series={chartSeries} height={240} />
      </div>
    </div>
  );
}
