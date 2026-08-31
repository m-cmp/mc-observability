const trimmed = (value) => String(value || '').trim();
const DEDICATED_FILTER_KEYS = new Set([
  'trace_id',
  'service_name',
  'endpoint',
  'status_code',
  'measurement',
  'ns_id',
  'infra_id',
  'node_id',
]);

export function buildRcaRequest(form = {}, context = {}) {
  const traceId = trimmed(form.traceId);
  const timeStart = trimmed(form.timeStart);
  const timeEnd = trimmed(form.timeEnd);

  if (Boolean(timeStart) !== Boolean(timeEnd)) {
    throw new Error('Start and end must be provided together.');
  }

  const attributes = {};
  if (context.nsId) attributes.ns_id = context.nsId;
  if (context.infraId) attributes.infra_id = context.infraId;
  if (context.nodeId) attributes.node_id = context.nodeId;
  if (trimmed(form.measurement)) attributes.measurement = trimmed(form.measurement);

  const scope = { attributes };
  if (traceId) scope.trace_id = traceId;
  if (trimmed(form.serviceName)) scope.service_name = trimmed(form.serviceName);
  if (trimmed(form.endpoint)) scope.endpoint = trimmed(form.endpoint);
  if (trimmed(form.statusCode)) scope.status_code = trimmed(form.statusCode);
  if (timeStart) {
    const start = new Date(timeStart);
    const end = new Date(timeEnd);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start >= end) {
      throw new Error('End must be later than start.');
    }
    scope.time_range = { start: start.toISOString(), end: end.toISOString() };
  }

  const filters = {};
  for (const filter of form.filters || []) {
    const key = trimmed(filter.key);
    const value = trimmed(filter.value);
    if (!key && !value) continue;
    if (!key || !value) {
      throw new Error('Each additional filter requires both a key and value.');
    }
    if (DEDICATED_FILTER_KEYS.has(key)) {
      throw new Error(`Use the dedicated field for "${key}".`);
    }
    if (Object.hasOwn(filters, key)) {
      throw new Error('Additional filter keys must be unique.');
    }
    filters[key] = value;
  }

  const request = { scope, filters };
  if (trimmed(form.query)) request.query = trimmed(form.query);
  if (form.connectionId) request.connection_id = Number(form.connectionId);
  if (trimmed(form.modelName)) request.model_name = trimmed(form.modelName);
  return request;
}

export function getRcaRecordView(record = {}) {
  const detail = record.detail || {};
  const result = detail.analysis_result || {};
  const request = record.request || {};
  const scope = request.scope || {};
  const validation = detail.result_validation || {};
  const noUsableEvidence = validation.usable_evidence_count === 0;

  return {
    request,
    result,
    validation,
    noUsableEvidence,
    cause: noUsableEvidence ? '' : result.probable_cause || record.summary || '',
    summary: result.summary || record.summary || '',
    service: result.affected_service || scope.service_name || '',
    endpoint: result.affected_endpoint || scope.endpoint || '',
    traceId: record.trace_id || scope.trace_id || '',
    riskLevel: result.risk_level || '',
    confidence: result.confidence,
    confidenceLabel: formatRcaConfidence(result.confidence),
    conclusionStrength: result.conclusion_strength || '',
    evidence: result.evidence || [],
    hypotheses: result.hypotheses || [],
    mitigation: result.mitigation || [],
    limitations: result.limitations || [],
    nextChecks: result.next_checks || [],
    errors: detail.errors || (detail.error_message ? [detail.error_message] : []),
    sources: detail.evidence_status || {},
  };
}

export function formatRcaConfidence(confidence) {
  if (typeof confidence === 'number') return `${Math.round(confidence * 100)}%`;
  if (typeof confidence === 'string' && confidence.trim()) {
    const value = confidence.trim();
    return value[0].toUpperCase() + value.slice(1).toLowerCase();
  }
  return '-';
}

export function formatRcaDuration(createdAt, updatedAt, status, now = Date.now()) {
  const start = new Date(createdAt).getTime();
  const end = ['PENDING', 'RUNNING'].includes(status)
    ? now
    : new Date(updatedAt).getTime();
  if (!Number.isFinite(start) || !Number.isFinite(end)) return '-';

  const totalSeconds = Math.max(0, Math.floor((end - start) / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours) return `${hours}h ${minutes}m`;
  if (minutes) return `${minutes}m ${seconds}s`;
  return `${seconds}s`;
}
