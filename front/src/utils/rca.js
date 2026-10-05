const trimmed = (value) => String(value || '').trim();

export function metricKeysByMeasurement(discovered) {
  const byKey = new Map();
  for (const { measurement, tags } of discovered) {
    for (const tag of tags || []) {
      if (!byKey.has(tag)) byKey.set(tag, new Set());
      byKey.get(tag).add(measurement);
    }
  }
  return Object.fromEntries([...byKey].sort(([a], [b]) => a.localeCompare(b))
    .map(([key, measurements]) => [key, [...measurements].sort()]));
}

export function compatibleMetricMeasurements(byKey, keys) {
  const selected = keys.filter(Boolean);
  if (!selected.length) return [];
  return (byKey[selected[0]] || []).filter((measurement) =>
    selected.every((key) => byKey[key]?.includes(measurement)));
}

export function buildRcaRequest(form = {}) {
  const timeStart = trimmed(form.timeStart);
  const timeEnd = trimmed(form.timeEnd);

  if (Boolean(timeStart) !== Boolean(timeEnd)) {
    throw new Error('Start and end must be provided together.');
  }

  const scope = {};
  for (const source of ['log', 'trace', 'metric']) {
    const values = {};
    for (const row of form.scopeRows?.[source] || []) {
      const key = trimmed(row.key);
      const value = trimmed(row.value);
      if (!key && !value) continue;
      if (!key || !value) throw new Error(`Choose both a ${source} key and value.`);
      if (Object.hasOwn(values, key)) throw new Error(`${source} keys must be unique.`);
      if (source === 'trace') {
        if (!trimmed(row.type)) throw new Error('Choose a trace value from the list.');
        values[key] = { value, type: row.type };
      } else {
        values[key] = value;
      }
    }
    if (Object.keys(values).length) scope[source] = values;
  }
  if (timeStart) {
    const start = new Date(timeStart);
    const end = new Date(timeEnd);
    if (Number.isNaN(start.getTime()) || Number.isNaN(end.getTime()) || start >= end) {
      throw new Error('End must be later than start.');
    }
    scope.time_range = { start: start.toISOString(), end: end.toISOString() };
  }

  const request = { scope };
  if (trimmed(form.query)) request.query = trimmed(form.query);
  if (form.connectionId) request.connection_id = Number(form.connectionId);
  if (trimmed(form.modelName)) request.model_name = trimmed(form.modelName);
  return request;
}

export function getRcaRecordView(record = {}) {
  const detail = record.detail || {};
  const result = detail.analysis_result || {};
  const request = record.request || {};
  const validation = detail.result_validation || {};
  const noUsableEvidence = validation.usable_evidence_count === 0;

  return {
    request,
    result,
    validation,
    noUsableEvidence,
    cause: noUsableEvidence ? '' : result.probable_cause || '',
    summary: result.summary || record.summary || '',
    service: result.affected_service || '',
    endpoint: result.affected_endpoint || '',
    traceId: record.trace_id || '',
    riskLevel: result.risk_level || '',
    confidence: result.confidence,
    confidenceLabel: formatRcaConfidence(result.confidence),
    conclusionStrength: result.conclusion_strength || '',
    evidence: result.evidence || [],
    nextChecks: result.next_checks || [],
    errors: validation.no_telemetry ? [] : detail.errors || (detail.error_message ? [detail.error_message] : []),
    sources: detail.evidence_status || {},
  };
}

// An evidence record is the tool output the validator restored; show JSON indented.
export function formatRawRecord(observation) {
  try {
    return JSON.stringify(JSON.parse(observation), null, 2);
  } catch {
    return String(observation);
  }
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

export const METRIC_ANOMALY_TRIGGER = 'metric_anomaly';

// Anomaly detection writes its interval as "5m" or "1h".
export function intervalMinutes(interval) {
  const match = /^(\d+)([mh])$/.exec(trimmed(interval));
  return match ? Number(match[1]) * (match[2] === 'h' ? 60 : 1) : null;
}

export function anomalySettingLabel(setting = {}) {
  return `#${setting.seq} · ${setting.ns_id}/${setting.infra_id}/${setting.node_id || 'all nodes'}`
    + ` · ${setting.measurement} · every ${setting.execution_interval}`;
}

// POST /rca/schedules body for a watch that follows one anomaly detection setting. It runs
// on each scoring of that setting; interval_minutes is only the API's required field.
export function buildAnomalyWatchSchedule({ name, setting, connectionId, modelName }) {
  if (!setting) throw new Error('Choose an anomaly detection setting.');
  return {
    name: trimmed(name),
    enabled: true,
    interval_minutes: Math.max(5, intervalMinutes(setting.execution_interval) || 5),
    trigger: METRIC_ANOMALY_TRIGGER,
    request: {
      connection_id: Number(connectionId),
      model_name: trimmed(modelName),
      anomaly_setting_seq: Number(setting.seq),
      scope: {},
    },
  };
}

// The Every, Trigger and Last status cells of the Automatic RCA list.
export function rcaScheduleCells(schedule = {}) {
  if (schedule.trigger === METRIC_ANOMALY_TRIGGER) {
    const seq = schedule.request?.anomaly_setting_seq;
    const status = { SKIPPED: 'No anomaly', SUCCEEDED: 'Analysis started' }[schedule.status] || schedule.status;
    return { every: 'on each scoring', trigger: `Metric anomaly #${seq}`, status };
  }
  if (schedule.trigger === 'server_error') {
    const traceScope = Object.entries(schedule.request?.scope?.trace || {})
      .map(([key, value]) => `${key}=${value.value}`).join(', ');
    return {
      every: `${schedule.interval_minutes}m`,
      trigger: traceScope ? `Server error · ${traceScope}` : 'Server error · all traces',
      status: schedule.status === 'SKIPPED' ? 'No server errors' : schedule.status,
    };
  }
  return { every: `${schedule.interval_minutes}m`, trigger: '-', status: schedule.status };
}
