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

// RCA — LLM-based root cause analysis over observability evidence.
export async function getRcaRecords({ status, from, to, page = 1, size = 20 } = {}) {
  const params = { page, size };
  if (status) params.status = status;
  if (from) params.from = from;
  if (to) params.to = to;
  const res = await client.get('/api/o11y/insight/rca/records', { params });
  return res.data?.data || {};
}

// Answers 202 with the record; the analysis runs on the server. Poll getRcaRecord for the result.
export async function queryRca(body) {
  const res = await client.post('/api/o11y/insight/rca/query', body);
  return res.data?.data || res.data;
}

export async function getRcaRecord(analysisId) {
  const res = await client.get(`/api/o11y/insight/rca/records/${analysisId}`);
  return res.data?.data || res.data;
}

// RCA schedules — the saved request body is replayed verbatim by Airflow.
export async function getRcaSchedules() {
  const res = await client.get('/api/o11y/insight/rca/schedules');
  return res.data?.data || [];
}

export async function createRcaSchedule(body) {
  const res = await client.post('/api/o11y/insight/rca/schedules', body);
  return res.data?.data || res.data;
}

export async function updateRcaSchedule(scheduleId, body) {
  const res = await client.patch(`/api/o11y/insight/rca/schedules/${scheduleId}`, body);
  return res.data?.data || res.data;
}

export async function deleteRcaSchedule(scheduleId) {
  return client.delete(`/api/o11y/insight/rca/schedules/${scheduleId}`);
}

// LLM
export async function getLlmConnections() {
  const res = await client.get('/api/o11y/insight/llm/connections');
  return res.data?.data || [];
}

export async function createLlmConnection(body) {
  const res = await client.post('/api/o11y/insight/llm/connections', body);
  return res.data?.data || res.data;
}

// Partial update: send only the changed fields. Changing provider/base_url drops the
// stored API key server-side, so the caller must resend api_key with that change.
export async function updateLlmConnection(connectionId, body) {
  const res = await client.patch(`/api/o11y/insight/llm/connections/${connectionId}`, body);
  return res.data?.data || res.data;
}

export async function setDefaultLlmConnection(connectionId, modelName) {
  const res = await client.put(`/api/o11y/insight/llm/connections/${connectionId}/default`, { model_name: modelName });
  return res.data?.data || res.data;
}

export async function deleteLlmConnection(connectionId) {
  return client.delete(`/api/o11y/insight/llm/connections/${connectionId}`);
}

export async function getLlmConnectionModels(connectionId) {
  const res = await client.get(`/api/o11y/insight/llm/connections/${connectionId}/models`);
  return res.data?.data || { connection_id: connectionId, models: [] };
}
