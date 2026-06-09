import client from './client';

// Backend insight URL paths use /ns/{nsId}/infra/{infraId}/node/{nodeId}
// (MCI→Infra, VM→Node naming applied).

// Anomaly Detection
export async function getAnomalySettings() {
  const res = await client.get('/api/o11y/insight/anomaly-detection/settings');
  return res.data?.data || [];
}

export async function createAnomalySetting(body) {
  const res = await client.post('/api/o11y/insight/anomaly-detection/settings', body);
  return res.data?.data || res.data;
}

export async function updateAnomalySetting(seq, body) {
  const res = await client.put(`/api/o11y/insight/anomaly-detection/settings/${seq}`, body);
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

export async function getAnomalyHistory(nsId, infraId, nodeId, measurement, startTime, endTime) {
  const params = { measurement };
  if (startTime) params.start_time = startTime;
  if (endTime) params.end_time = endTime;
  const path = nodeId
    ? `/api/o11y/insight/anomaly-detection/ns/${nsId}/infra/${infraId}/node/${nodeId}/history`
    : `/api/o11y/insight/anomaly-detection/ns/${nsId}/infra/${infraId}/history`;
  const res = await client.get(path, { params });
  return res.data?.data || res.data?.responseData || {};
}

// Prediction
export async function getPredictionOptions() {
  const res = await client.get('/api/o11y/insight/predictions/options');
  return res.data?.data || {};
}

export async function getPredictionHistory(nsId, infraId, nodeId, measurement, startTime, endTime) {
  const params = { measurement };
  if (startTime) params.start_time = startTime;
  if (endTime) params.end_time = endTime;
  const path = nodeId
    ? `/api/o11y/insight/predictions/ns/${nsId}/infra/${infraId}/node/${nodeId}/history`
    : `/api/o11y/insight/predictions/ns/${nsId}/infra/${infraId}/history`;
  const res = await client.get(path, { params });
  return res.data?.data || res.data?.responseData || {};
}

export async function runPrediction(nsId, infraId, nodeId, body) {
  const path = nodeId
    ? `/api/o11y/insight/predictions/ns/${nsId}/infra/${infraId}/node/${nodeId}`
    : `/api/o11y/insight/predictions/ns/${nsId}/infra/${infraId}`;
  const res = await client.post(path, body);
  return res.data?.data || res.data?.responseData || {};
}

export async function getPredictionMeasurements() {
  const res = await client.get('/api/o11y/insight/predictions/measurement');
  return res.data?.data || [];
}

// Server Error Analysis (#300) — LLM-based 5xx analysis over Tempo traces.
export async function getServerErrorRecords({ status, from, to, page = 1, size = 20 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (from) params.from = from;
  if (to) params.to = to;
  const res = await client.get('/api/o11y/insight/server-error-analysis/records', { params });
  return res.data?.data || {};
}

export async function getServerErrorRecord(analysisId) {
  const res = await client.get(`/api/o11y/insight/server-error-analysis/records/${analysisId}`);
  return res.data?.data || res.data;
}

export async function detectServerError(body) {
  const res = await client.post('/api/o11y/insight/server-error-analysis/detect', body);
  return res.data?.data || res.data;
}

export async function queryServerError(body) {
  const res = await client.post('/api/o11y/insight/server-error-analysis/query', body);
  return res.data?.data || res.data;
}

export async function rerunServerErrorAnalysis(analysisId) {
  const res = await client.post(
    `/api/o11y/insight/server-error-analysis/records/${analysisId}/rerun`,
  );
  return res.data?.data || res.data;
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
