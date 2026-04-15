import client from './client';

// Based on cb-spider MonitoringHandler interface (MonitoringHandler.go)
// MetricType enum: cpu_usage, memory_usage, disk_read, disk_write, disk_read_ops, disk_write_ops, network_in, network_out
// Response: MetricData { metricName, metricUnit, timestampValues: [{timestamp, value}] }
// API: GET /spider/monitoring/vm/:VMName/:MetricType?ConnectionName=...&periodMinute=...&timeBeforeHour=...

const CSP_METRICS = [
  { key: 'cpu_usage', label: 'CPU Usage', unit: '%' },
  { key: 'memory_usage', label: 'Memory Available', unit: 'bytes' },
  { key: 'disk_read', label: 'Disk Read', unit: 'bytes' },
  { key: 'disk_write', label: 'Disk Write', unit: 'bytes' },
  { key: 'disk_read_ops', label: 'Disk Read Ops', unit: 'count' },
  { key: 'disk_write_ops', label: 'Disk Write Ops', unit: 'count' },
  { key: 'network_in', label: 'Network In', unit: 'bytes' },
  { key: 'network_out', label: 'Network Out', unit: 'bytes' },
];

export { CSP_METRICS };

/**
 * @param {string} connectionName - cb-spider connection name (e.g. "azure-koreacentral")
 * @param {string} cspResourceName - CSP-level VM name (from Tumblebug vm.cspResourceName)
 * @param {string} metricType - one of CSP_METRICS keys
 * @param {string} periodMinute - aggregation interval (default "5")
 * @param {string} timeBeforeHour - lookback hours (default "1")
 */
export async function getCspMetric(connectionName, cspResourceName, metricType, periodMinute = '5', timeBeforeHour = '1') {
  const res = await client.get(`/spider/monitoring/vm/${cspResourceName}/${metricType}`, {
    params: { ConnectionName: connectionName, periodMinute, timeBeforeHour },
  });
  return res.data || {};
}

/** Fetch all CSP metrics for a VM. Returns { cpu_usage: {metricName, metricUnit, series}, ... } */
export async function getAllCspMetrics(connectionName, cspResourceName, timeBeforeHour = '1') {
  const results = {};
  await Promise.allSettled(
    CSP_METRICS.map(async (m) => {
      const data = await getCspMetric(connectionName, cspResourceName, m.key, '5', timeBeforeHour);
      results[m.key] = {
        ...m,
        metricName: data.metricName || m.label,
        metricUnit: data.metricUnit || m.unit,
        series: [{
          name: data.metricName || m.label,
          data: (data.timestampValues || []).map((v) => ({
            x: new Date(v.timestamp).getTime(),
            y: parseFloat(v.value),
          })),
        }],
      };
    })
  );
  return results;
}

// Supported CSP providers for monitoring (Azure, AWS, GCP)
export const CSP_SUPPORTED_PROVIDERS = ['azure', 'aws', 'gcp'];

export function isCspSupported(connectionName) {
  if (!connectionName) return false;
  const lower = connectionName.toLowerCase();
  return CSP_SUPPORTED_PROVIDERS.some((p) => lower.includes(p));
}
