import client from './client';

// NOTE: backend URL path still uses /mci/{}/vm/{} literals (URL segments unchanged).
// InfluxDB tags and JS identifiers follow the Tumblebug rename (MCI→Infra, VM→Node).

export async function getMeasurementFields() {
  const res = await client.get('/api/o11y/monitoring/influxdb/measurement');
  return res.data?.data || [];
}

export async function getPlugins() {
  const res = await client.get('/api/o11y/monitoring/plugins');
  return res.data?.data || [];
}

export async function getMetricsByNode(nsId, infraId, nodeId, { measurement, range, groupTime, fields, conditions }) {
  const res = await client.post(`/api/o11y/monitoring/influxdb/metric/${nsId}/${infraId}/${nodeId}`, {
    measurement,
    range,
    group_time: groupTime,
    group_by: ['node_id'],
    limit: 2000,
    fields: fields || [],
    conditions: conditions || [],
  });
  return res.data?.data || [];
}

export async function getMetricsByNsInfra(nsId, infraId, { measurement, range, groupTime, fields, conditions }) {
  const res = await client.post(`/api/o11y/monitoring/influxdb/metric/${nsId}/${infraId}`, {
    measurement,
    range,
    group_time: groupTime,
    group_by: ['node_id'],
    limit: 2000,
    fields: fields || [],
    conditions: conditions || [],
  });
  return res.data?.data || [];
}

export async function getPrediction(nsId, infraId, nodeId, measurement) {
  const now = new Date();
  const startTime = new Date(now.getTime() - 12 * 60 * 60 * 1000).toISOString();
  const endTime = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString();
  const res = await client.post(`/api/o11y/insight/predictions/ns/${nsId}/mci/${infraId}/vm/${nodeId}`, {
    measurement,
    start_time: startTime,
    end_time: endTime,
  });
  return res.data?.data || res.data?.responseData || {};
}

export async function getDetectionHistory(nsId, infraId, nodeId, measurement) {
  const now = new Date();
  const startTime = new Date(now.getTime() - 12 * 60 * 60 * 1000).toISOString();
  const endTime = now.toISOString();
  const res = await client.get(
    `/api/o11y/insight/anomaly-detection/ns/${nsId}/mci/${infraId}/vm/${nodeId}/history`,
    { params: { measurement, start_time: startTime, end_time: endTime } }
  );
  return res.data?.data || res.data?.responseData || {};
}
