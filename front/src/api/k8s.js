import client from './client';
import { CSP_METRICS } from './csp';

export async function getClusters(connectionName) {
  const res = await client.get('/spider/cluster', { params: { ConnectionName: connectionName } });
  const data = res.data || {};
  return data.cluster || [];
}

export async function getCluster(connectionName, clusterName) {
  const res = await client.get(`/spider/cluster/${clusterName}`, { params: { ConnectionName: connectionName } });
  return res.data || {};
}

export async function getClusterNodeMetric(connectionName, clusterName, nodeGroupName, nodeNumber, metricType, periodMinute = '5', timeBeforeHour = '1') {
  const res = await client.get(
    `/spider/monitoring/clusternode/${clusterName}/${nodeGroupName}/${nodeNumber}/${metricType}`,
    { params: { ConnectionName: connectionName, periodMinute, timeBeforeHour } }
  );
  return res.data || {};
}

/** Fetch all CSP metrics for a single cluster node. Response shape matches getAllCspMetrics. */
export async function getAllClusterNodeMetrics(connectionName, clusterName, nodeGroupName, nodeNumber, timeBeforeHour = '1') {
  const results = {};
  await Promise.allSettled(
    CSP_METRICS.map(async (m) => {
      const data = await getClusterNodeMetric(connectionName, clusterName, nodeGroupName, nodeNumber, m.key, '5', timeBeforeHour);
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
