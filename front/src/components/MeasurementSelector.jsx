const RANGE_OPTIONS = [
  { value: '1h', label: '1H' },
  { value: '6h', label: '6H' },
  { value: '12h', label: '12H' },
  { value: '1d', label: '1D' },
  { value: '3d', label: '3D' },
  { value: '5d', label: '5D' },
  { value: '7d', label: '7D' },
];

const PERIOD_MAP = {
  '1h': '1m',
  '6h': '5m',
  '12h': '5m',
  '1d': '5m',
  '3d': '15m',
  '5d': '30m',
  '7d': '1h',
};

export { RANGE_OPTIONS, PERIOD_MAP };

export default function MeasurementSelector({
  measurements,
  selectedMeasurement,
  onMeasurementChange,
  selectedRange,
  onRangeChange,
  onRefresh,
  loading,
}) {
  return (
    <div className="flex items-center gap-3 flex-wrap">
      <select
        className="border border-gray-300 rounded-md px-3 py-1.5 text-sm bg-white"
        value={selectedMeasurement}
        onChange={(e) => onMeasurementChange(e.target.value)}
      >
        <option value="">Measurement</option>
        {measurements.map((m) => (
          <option key={m} value={m}>{m}</option>
        ))}
      </select>

      <div className="flex gap-1">
        {RANGE_OPTIONS.map((opt) => (
          <button
            key={opt.value}
            onClick={() => onRangeChange(opt.value)}
            className={`px-2.5 py-1 text-xs rounded-md border transition-colors ${
              selectedRange === opt.value
                ? 'bg-blue-600 text-white border-blue-600'
                : 'bg-white text-gray-600 border-gray-300 hover:bg-gray-50'
            }`}
          >
            {opt.label}
          </button>
        ))}
      </div>

      <button
        onClick={onRefresh}
        disabled={loading}
        className="px-3 py-1.5 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
      >
        {loading ? 'Loading...' : 'Refresh'}
      </button>

      <span className="text-xs text-gray-400 ml-auto">
        Period: {PERIOD_MAP[selectedRange] || '1m'}
      </span>
    </div>
  );
}
