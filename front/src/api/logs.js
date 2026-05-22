import client from './client';

// NOTE: Loki labels (NS_ID / MCI_ID / VM_ID) stay as-is — backend label rename
// pending. JS identifiers reflect Tumblebug Infra/Node naming.

export async function queryLogs({ nsId, infraId, nodeId, keyword, limit = 50, rangeHours = 24 }) {
  let logql = `{NS_ID="${nsId}", MCI_ID="${infraId}"`;
  if (nodeId) logql += `, VM_ID="${nodeId}"`;
  logql += '}';
  if (keyword) logql += ` |~ "(?i)${keyword}"`;

  const now = new Date();
  const start = new Date(now.getTime() - rangeHours * 60 * 60 * 1000);

  const res = await client.get('/api/o11y/log/query_range', {
    params: {
      query: logql,
      limit: String(limit),
      start: Math.floor(start.getTime() / 1000).toString(),
      end: Math.floor(now.getTime() / 1000).toString(),
    },
  });
  const body = res.data?.data || res.data || {};
  // API returns { status, data: [...] } where each entry has { labels, timestamp, value(json string) }
  const entries = body.data || body || [];
  if (!Array.isArray(entries)) return [];
  return entries.map((e) => {
    let parsed = {};
    try { parsed = typeof e.value === 'string' ? JSON.parse(e.value) : (e.value || {}); } catch {}
    return {
      timestamp: parsed.time || (e.timestamp ? new Date(e.timestamp / 1e6).toISOString() : ''),
      message: parsed.message || '',
      level: e.labels?.level || parsed.level || '',
      node_id: e.labels?.VM_ID || '',
      host: e.labels?.host || parsed.host || '',
      service: parsed.service || e.labels?.service || '',
      source: parsed.source || e.labels?.source || '',
      labels: e.labels || {},
      raw: parsed,
    };
  });
}

export async function getLogLabels() {
  const res = await client.get('/api/o11y/log/labels');
  return res.data?.data || [];
}

export async function getLogLabelValues(label) {
  const res = await client.get(`/api/o11y/log/labels/${label}/values`);
  return res.data?.data || [];
}
