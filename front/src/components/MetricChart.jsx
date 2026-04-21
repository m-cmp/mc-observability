import { useRef, useEffect } from 'react';
import Chart from 'react-apexcharts';

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#84cc16'];

/**
 * @param {string} measurement - e.g. "cpu", "disk", "mem", "net", "system"
 * @param {string} metric - e.g. "used", "used_percent", "usage_idle", "bytes_recv"
 */
export default function MetricChart({ title, series, height = 240, chartType = 'area', measurement, metric }) {
  if (!series || series.length === 0) {
    return (
      <div className="flex items-center justify-center h-40 text-gray-400 text-sm">
        No data
      </div>
    );
  }

  const unit = detectUnit(measurement, metric);
  const formatter = buildFormatter(unit);
  const yTitle = unit.label ? `(${unit.label})` : '';

  const options = {
    chart: { type: chartType, toolbar: { show: true }, zoom: { enabled: true, type: 'x', autoScaleYaxis: true }, animations: { enabled: false } },
    title: { text: title + (yTitle ? ` ${yTitle}` : ''), align: 'center', style: { fontSize: '14px', fontWeight: 600 } },
    xaxis: { type: 'datetime', labels: { format: 'HH:mm:ss', style: { fontSize: '11px' }, datetimeUTC: false } },
    yaxis: {
      labels: { formatter, style: { fontSize: '11px' } },
      title: { text: unit.label || '', style: { fontSize: '11px' } },
    },
    fill: { type: 'solid', opacity: 0.12 },
    stroke: { curve: 'smooth', width: 2 },
    colors: COLORS.slice(0, series.length),
    legend: { show: true, position: 'top', horizontalAlign: 'left', fontSize: '11px' },
    tooltip: {
      theme: 'dark',
      x: { format: 'yyyy-MM-dd HH:mm:ss' },
      y: { formatter },
    },
    grid: { strokeDashArray: 4 },
    dataLabels: { enabled: false },
  };

  const containerRef = useRef(null);
  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;
    const handler = (e) => e.preventDefault();
    el.addEventListener('wheel', handler, { passive: false });
    return () => el.removeEventListener('wheel', handler);
  }, []);

  return (
    <div ref={containerRef}>
      <Chart options={options} series={series} type={chartType} height={height} />
    </div>
  );
}

// --- Unit detection ---

const BYTE_FIELDS = [
  'used', 'free', 'total', 'available', 'active', 'inactive', 'buffered', 'cached', 'shared', 'slab',
  'bytes_recv', 'bytes_sent',
  'read_bytes', 'write_bytes',
];
const PERCENT_FIELDS = [
  'used_percent', 'usage_idle', 'usage_user', 'usage_system', 'usage_iowait',
  'usage_irq', 'usage_softirq', 'usage_steal', 'usage_guest', 'usage_nice',
  'inodes_used_percent',
];
const TIME_FIELDS = ['uptime', 'server_time'];

function detectUnit(measurement, metric) {
  const m = (metric || '').toLowerCase();
  const meas = (measurement || '').toLowerCase();

  if (PERCENT_FIELDS.includes(m) || m.includes('percent')) {
    return { type: 'percent', label: '%' };
  }
  if (TIME_FIELDS.includes(m)) {
    return { type: 'duration', label: 'sec' };
  }
  if (BYTE_FIELDS.includes(m)) {
    if (meas === 'net' || meas === 'diskio') {
      return { type: 'bytes_rate', label: 'bytes/s' };
    }
    return { type: 'bytes', label: 'bytes' };
  }
  return { type: 'number', label: '' };
}

function buildFormatter(unit) {
  switch (unit.type) {
    case 'percent':
      return (v) => (v != null ? v.toFixed(1) + '%' : '');
    case 'bytes':
      return formatBytes;
    case 'bytes_rate':
      return (v) => formatBytes(v) + '/s';
    case 'duration':
      return formatDuration;
    default:
      return (v) => (v != null ? smartNumber(v) : '');
  }
}

function formatBytes(v) {
  if (v == null) return '';
  const abs = Math.abs(v);
  if (abs >= 1e12) return (v / 1e12).toFixed(2) + ' TB';
  if (abs >= 1e9) return (v / 1e9).toFixed(2) + ' GB';
  if (abs >= 1e6) return (v / 1e6).toFixed(2) + ' MB';
  if (abs >= 1e3) return (v / 1e3).toFixed(1) + ' KB';
  return v.toFixed(0) + ' B';
}

function formatDuration(v) {
  if (v == null) return '';
  if (v >= 86400) return (v / 86400).toFixed(1) + 'd';
  if (v >= 3600) return (v / 3600).toFixed(1) + 'h';
  if (v >= 60) return (v / 60).toFixed(1) + 'm';
  return v.toFixed(0) + 's';
}

function smartNumber(v) {
  if (v == null) return '';
  const abs = Math.abs(v);
  if (abs >= 1e9) return (v / 1e9).toFixed(2) + 'G';
  if (abs >= 1e6) return (v / 1e6).toFixed(2) + 'M';
  if (abs >= 1e3) return (v / 1e3).toFixed(1) + 'K';
  if (Number.isInteger(v)) return v.toString();
  return v.toFixed(2);
}
