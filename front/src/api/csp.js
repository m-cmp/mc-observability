import client from './client';

// Based on cb-spider MonitoringHandler interface (MonitoringHandler.go)
const CSP_METRICS = [
  { key: 'cpu_usage', label: 'CPU Usage', unit: '%' },
  { key: 'memory_usage', label: 'Memory Usage', unit: '%' },
  { key: 'disk_read', label: 'Disk Read', unit: 'bytes' },
  { key: 'disk_write', label: 'Disk Write', unit: 'bytes' },
  { key: 'disk_read_ops', label: 'Disk Read Ops', unit: 'count' },
  { key: 'disk_write_ops', label: 'Disk Write Ops', unit: 'count' },
  { key: 'network_in', label: 'Network In', unit: 'bytes' },
  { key: 'network_out', label: 'Network Out', unit: 'bytes' },
];

export { CSP_METRICS };

export async function getCspMetric(connectionName, cspResourceName, metricType, periodMinute = '5', timeBeforeHour = '1') {
  const res = await client.get(`/spider/monitoring/vm/${cspResourceName}/${metricType}`, {
    params: { ConnectionName: connectionName, periodMinute, timeBeforeHour },
  });
  return res.data || {};
}

/**
 * Fetch all CSP metrics for a VM.
 * @param {number} totalMemoryGiB - VM total memory in GiB (from Tumblebug spec.memoryGiB). Used to convert Available Memory Bytes → %.
 */
export async function getAllCspMetrics(connectionName, cspResourceName, timeBeforeHour = '1', totalMemoryGiB = 0) {
  const results = {};
  const totalMemoryBytes = totalMemoryGiB * 1024 * 1024 * 1024;

  await Promise.allSettled(
    CSP_METRICS.map(async (m) => {
      const data = await getCspMetric(connectionName, cspResourceName, m.key, '5', timeBeforeHour);
      let metricName = data.metricName || m.label;
      let metricUnit = data.metricUnit || m.unit;
      let points = (data.timestampValues || []).map((v) => ({
        x: new Date(v.timestamp).getTime(),
        y: parseFloat(v.value),
      }));

      // Convert "Available Memory Bytes" → "Memory Usage %"
      if (m.key === 'memory_usage' && totalMemoryBytes > 0 && metricUnit === 'Bytes') {
        points = points.map((p) => ({
          ...p,
          y: Math.max(0, (1 - p.y / totalMemoryBytes) * 100),
        }));
        metricName = 'Memory Usage';
        metricUnit = 'Percent';
      }

      results[m.key] = {
        ...m,
        metricName,
        metricUnit,
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
