import client from './client';

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

export async function getCspMetric(connectionName, cspResourceName, metricType, periodMinute = '5', timeBeforeHour = '1') {
  const res = await client.get(`/spider/monitoring/vm/${cspResourceName}/${metricType}`, {
    params: { ConnectionName: connectionName, periodMinute, timeBeforeHour },
  });
  return res.data || {};
}

/** Fetch all CSP metrics for a VM. */
export async function getAllCspMetrics(connectionName, cspResourceName, timeBeforeHour = '1') {
  const results = {};
  await Promise.allSettled(
    CSP_METRICS.map(async (m) => {
      const data = await getCspMetric(connectionName, cspResourceName, m.key, '5', timeBeforeHour);
      const metricName = data.metricName || m.label;
      const metricUnit = data.metricUnit || m.unit;
      const points = (data.timestampValues || []).map((v) => ({
        x: new Date(v.timestamp).getTime(),
        y: parseFloat(v.value),
      }));
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
