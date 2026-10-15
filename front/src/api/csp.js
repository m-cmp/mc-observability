import client from './client';
import { toEpochMillis } from '../utils/time';

// Based on cb-spider MonitoringHandler interface (MonitoringHandler.go)
// Aligned with cb-spider MetricNameAndUnit() in MonitoringHandler.go
const CSP_METRICS = [
  { key: 'cpu_usage', label: 'CPU Usage Percent', unit: 'Percent' },
  { key: 'memory_usage', label: 'Memory Usage Percent', unit: 'Percent' },
  { key: 'disk_read', label: 'Disk Read Bytes', unit: 'Bytes' },
  { key: 'disk_write', label: 'Disk Write Bytes', unit: 'Bytes' },
  { key: 'disk_read_ops', label: 'Disk Read Operations/Sec', unit: 'CountPerSecond' },
  { key: 'disk_write_ops', label: 'Disk Write Operations/Sec', unit: 'CountPerSecond' },
  { key: 'network_in', label: 'Network In', unit: 'Bytes' },
  { key: 'network_out', label: 'Network Out', unit: 'Bytes' },
];

export { CSP_METRICS };

/**
 * cb-spider returns only the points that have data, so a span without data (the VM was off)
 * would be drawn as a line joining its two ends. Put a null wherever two points are further apart
 * than 1.5 periods, and at the window's start and end when the data stops short of them, so the
 * chart shows those spans empty.
 */
export function withGaps(points, periodMinute, timeBeforeHour) {
  const step = Number(periodMinute) * 60 * 1000;
  const sorted = points.filter((p) => !Number.isNaN(p.x)).sort((a, b) => a.x - b.x);
  const end = Date.now();
  const start = end - Number(timeBeforeHour) * 60 * 60 * 1000;
  const out = [];
  let prev = start;
  for (const p of sorted) {
    if (p.x - prev > step * 1.5) out.push({ x: prev + step, y: null });
    out.push(p);
    prev = p.x;
  }
  if (end - prev > step * 1.5) out.push({ x: prev + step, y: null }, { x: end, y: null });
  return out;
}

export async function getCspMetric(connectionName, cspResourceName, metricType, periodMinute = '5', timeBeforeHour = '1', signal) {
  const res = await client.get(`/api/o11y/monitoring/csp/node/${cspResourceName}/${metricType}`, {
    params: { ConnectionName: connectionName, periodMinute, timeBeforeHour },
    signal,
  });
  return res.data || {};
}

/** Fetch all CSP metrics for a Node. */
export async function getAllCspMetrics(connectionName, cspResourceName, timeBeforeHour = '1', signal) {
  const results = {};
  await Promise.allSettled(
    CSP_METRICS.map(async (m) => {
      const data = await getCspMetric(connectionName, cspResourceName, m.key, '5', timeBeforeHour, signal);
      const metricName = data.metricName || m.label;
      const metricUnit = data.metricUnit || m.unit;
      const points = withGaps((data.timestampValues || []).map((v) => ({
        x: toEpochMillis(v.timestamp),
        y: parseFloat(v.value),
      })), '5', timeBeforeHour);
      results[m.key] = {
        ...m,
        metricName,
        metricUnit,
        unsupported: data.unsupported === true,
        series: [{ name: metricName, data: points }],
      };
    })
  );
  return results;
}

export const CSP_SUPPORTED_PROVIDERS = ['azure', 'aws', 'gcp'];

export function isCspSupported(connectionName) {
  if (!connectionName) return false;
  const lower = connectionName.toLowerCase();
  return CSP_SUPPORTED_PROVIDERS.some((p) => lower.includes(p));
}
