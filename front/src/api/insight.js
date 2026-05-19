import client from './client';

// Anomaly Detection
export async function getAnomalySettings() {
  const res = await client.get('/api/o11y/insight/anomaly-detection/settings');
  return res.data?.data || [];
}

export async function createAnomalySetting(body) {
  const res = await client.post('/api/o11y/insight/anomaly-detection/settings', body);
  return res.data?.data || res.data;
}

export async function deleteAnomalySetting(seq) {
  return client.delete(`/api/o11y/insight/anomaly-detection/settings/${seq}`);
}

export async function getAnomalyOptions() {
  const res = await client.get('/api/o11y/insight/anomaly-detection/options');
  return res.data?.data || {};
}

export async function getAnomalyMeasurements() {
  const res = await client.get('/api/o11y/insight/anomaly-detection/measurement');
  return res.data?.data || [];
}

export async function getAnomalyHistory(nsId, mciId, vmId, measurement, startTime, endTime) {
  const params = { measurement };
  if (startTime) params.start_time = startTime;
  if (endTime) params.end_time = endTime;
  const path = vmId
    ? `/api/o11y/insight/anomaly-detection/ns/${nsId}/mci/${mciId}/vm/${vmId}/history`
    : `/api/o11y/insight/anomaly-detection/ns/${nsId}/mci/${mciId}/history`;
  const res = await client.get(path, { params });
  return res.data?.data || res.data?.responseData || {};
}

// Prediction
export async function getPredictionOptions() {
  const res = await client.get('/api/o11y/insight/predictions/options');
  return res.data?.data || {};
}

export async function getPredictionHistory(nsId, mciId, vmId, measurement, startTime, endTime) {
  const params = { measurement };
  if (startTime) params.start_time = startTime;
  if (endTime) params.end_time = endTime;
  const path = vmId
    ? `/api/o11y/insight/predictions/ns/${nsId}/mci/${mciId}/vm/${vmId}/history`
    : `/api/o11y/insight/predictions/ns/${nsId}/mci/${mciId}/history`;
  const res = await client.get(path, { params });
  return res.data?.data || res.data?.responseData || {};
}

export async function runPrediction(nsId, mciId, vmId, body) {
  const path = vmId
    ? `/api/o11y/insight/predictions/ns/${nsId}/mci/${mciId}/vm/${vmId}`
    : `/api/o11y/insight/predictions/ns/${nsId}/mci/${mciId}`;
  const res = await client.post(path, body);
  return res.data?.data || res.data?.responseData || {};
}

// LLM
export async function getLlmModels() {
  const res = await client.get('/api/o11y/insight/llm/model');
  return res.data?.data || [];
}

export async function getLlmSessions() {
  const res = await client.get('/api/o11y/insight/llm/session');
  return res.data?.data || [];
}

export async function createLlmSession(body) {
  const res = await client.post('/api/o11y/insight/llm/session', body);
  return res.data?.data || res.data;
}

export async function getLlmHistory(sessionId) {
  const res = await client.get(`/api/o11y/insight/llm/session/${sessionId}/history`);
  return res.data?.data || [];
}
