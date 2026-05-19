import client from './client';

export async function getMeasurementFields() {
  const res = await client.get('/api/o11y/monitoring/influxdb/measurement');
  return res.data?.data || [];
}

export async function getPlugins() {
  const res = await client.get('/api/o11y/monitoring/plugins');
  return res.data?.data || [];
}

export async function getMetricsByVM(nsId, mciId, vmId, { measurement, range, groupTime, fields, conditions }) {
  const res = await client.post(`/api/o11y/monitoring/influxdb/metric/${nsId}/${mciId}/${vmId}`, {
    measurement,
    range,
    group_time: groupTime,
    group_by: ['vm_id'],
    limit: 2000,
    fields: fields || [],
    conditions: conditions || [],
  });
  return res.data?.data || [];
}

export async function getMetricsByNsMci(nsId, mciId, { measurement, range, groupTime, fields, conditions }) {
  const res = await client.post(`/api/o11y/monitoring/influxdb/metric/${nsId}/${mciId}`, {
    measurement,
    range,
    group_time: groupTime,
    group_by: ['vm_id'],
    limit: 2000,
    fields: fields || [],
    conditions: conditions || [],
  });
  return res.data?.data || [];
}

export async function getPrediction(nsId, mciId, vmId, measurement) {
  const now = new Date();
  const startTime = new Date(now.getTime() - 12 * 60 * 60 * 1000).toISOString();
  const endTime = new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString();
  const res = await client.post(`/api/o11y/insight/predictions/ns/${nsId}/mci/${mciId}/vm/${vmId}`, {
    measurement,
    start_time: startTime,
    end_time: endTime,
  });
  return res.data?.data || res.data?.responseData || {};
}

export async function getDetectionHistory(nsId, mciId, vmId, measurement) {
  const now = new Date();
  const startTime = new Date(now.getTime() - 12 * 60 * 60 * 1000).toISOString();
  const endTime = now.toISOString();
  const res = await client.get(
    `/api/o11y/insight/anomaly-detection/ns/${nsId}/mci/${mciId}/vm/${vmId}/history`,
    { params: { measurement, start_time: startTime, end_time: endTime } }
  );
  return res.data?.data || res.data?.responseData || {};
}
