import client from './client';

// Tempo trace query. The backend builds the TraceQL from scope/service/keyword and
// a time window. `scope` splits framework (o11y platform: manager/insight, self-traced
// via the OTel agent) from vm (target VMs' Beyla/OTel application traces).

export async function searchTraces({ scope, service, keyword, rangeHours = 1, limit = 100 } = {}) {
  const now = new Date();
  const start = new Date(now.getTime() - rangeHours * 60 * 60 * 1000);
  const params = {
    start: Math.floor(start.getTime() / 1000).toString(),
    end: Math.floor(now.getTime() / 1000).toString(),
    limit: String(limit),
  };
  if (scope) params.scope = scope;
  if (service) params.service = service;
  if (keyword) params.keyword = keyword;

  const res = await client.get('/api/o11y/trace/search', { params });
  return res.data?.data || [];
}

export async function getTraceServices(scope) {
  const params = {};
  if (scope) params.scope = scope;
  const res = await client.get('/api/o11y/trace/services', { params });
  return res.data?.data || [];
}

export async function getTrace(traceId) {
  const res = await client.get(`/api/o11y/trace/${traceId}`);
  return res.data?.data || null;
}
