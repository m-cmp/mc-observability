import client from './client';

// NOTE: Loki labels renamed to NS_ID / INFRA_ID / NODE_ID (fluent-bit label rename
// applied). JS identifiers reflect Tumblebug Infra/Node naming.

// Best-effort level from a log line: bracketed tags, klog (I/W/E/F0623…), or a bare keyword.
function deriveLevel(msg) {
  if (!msg) return '';
  const b = msg.match(/^\s*\[(TRACE|DEBUG|INFO|NOTICE|WARN|WARNING|ERROR|FATAL|CRIT|CRITICAL)\]/i);
  if (b) return b[1].toUpperCase();
  // klog severity token "<I|W|E|F>MMDD HH:MM:SS" — at the start (container logs) or embedded
  // after a syslog prefix (e.g. "kubelet[4168]: E0623 05:15:04 …").
  const k = msg.match(/(?:^|\s)([IWEF])\d{4}\s+\d{2}:\d{2}:\d{2}/);
  if (k) return { I: 'INFO', W: 'WARN', E: 'ERROR', F: 'FATAL' }[k[1]];
  const w = msg.match(/\b(TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL)\b/);
  if (w) return w[1].toUpperCase();
  return '';
}

// Best-effort service from a syslog-style line: "Mon DD HH:MM:SS host <proc>[pid]: …".
function deriveService(msg) {
  if (!msg) return '';
  const m = msg.match(/^[A-Z][a-z]{2}\s+\d+\s+[\d:]+\s+\S+\s+([\w.-]+?)(?:\[\d+\])?:\s/);
  return m ? m[1] : '';
}

export async function queryLogs({ nsId, infraId, nodeId, keyword, limit = 50, rangeHours = 24 }) {
  let logql = `{NS_ID="${nsId}", INFRA_ID="${infraId}"`;
  if (nodeId) logql += `, NODE_ID="${nodeId}"`;
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
    let isJson = false;
    try {
      if (typeof e.value === 'string') { parsed = JSON.parse(e.value); isJson = true; }
      else if (e.value) parsed = e.value;
    } catch { parsed = {}; }
    // VM agent ships structured JSON with a `message` key. The K8s agent (fluent-bit tailing
    // container/syslog files) ships the raw line under `log`; container logs are CRI-formatted
    // ("<ts> stdout|stderr F <msg>") so strip that prefix to show the real message.
    let message = parsed.message;
    if (!message) {
      if (typeof parsed.log === 'string') {
        const m = parsed.log.match(/^\S+\s+std(?:out|err)\s+[FP]\s+([\s\S]*)$/);
        message = m ? m[1] : parsed.log;
      } else if (!isJson && typeof e.value === 'string') {
        message = e.value; // plain (non-JSON) log line
      }
    }
    message = (message || '').replace(/\s+$/, '');
    return {
      timestamp: parsed.time || (e.timestamp ? new Date(e.timestamp / 1e6).toISOString() : ''),
      message,
      level: e.labels?.level || parsed.level || deriveLevel(message) || 'UNKNOWN',
      node_id: e.labels?.NODE_ID || '',
      host: e.labels?.host || parsed.host || '',
      service: parsed.service || e.labels?.service || deriveService(message),
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
