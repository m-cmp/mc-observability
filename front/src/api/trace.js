import client from './client';

// Tempo trace query. The backend builds the TraceQL from service/keyword and a
// time window; traces are emitted by the Beyla / OTel trace agents and stored
// in Tempo (OTLP ingest). Mirrors the logs.js shape: thin wrappers over the
// manager REST API.

export async function searchTraces({ service, keyword, rangeHours = 1, limit = 100 } = {}) {
  const now = new Date();
  const start = new Date(now.getTime() - rangeHours * 60 * 60 * 1000);
  const params = {
    start: Math.floor(start.getTime() / 1000).toString(),
    end: Math.floor(now.getTime() / 1000).toString(),
    limit: String(limit),
  };
  if (service) params.service = service;
  if (keyword) params.keyword = keyword;

  const res = await client.get('/api/o11y/trace/search', { params });
  return res.data?.data || [];
}

export async function getTrace(traceId) {
  const res = await client.get(`/api/o11y/trace/${traceId}`);
  return res.data?.data || null;
}
