import client from './client';

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
