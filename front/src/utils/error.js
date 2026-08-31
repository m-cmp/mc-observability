/**
 * Message to show for a failed API call. The backends disagree on the field name
 * (FastAPI `detail`, o11y-manager `error_message`, tumblebug `rs_msg`), so pick
 * whichever one came back instead of repeating the chain at every call site.
 */
export function apiError(e, fallback = 'Request failed.') {
  const data = e?.response?.data || {};
  const raw = data.detail ?? data.error_message ?? data.rs_msg ?? data.message ?? e?.message;
  // FastAPI validation errors arrive as a list of {loc, msg, type}.
  if (Array.isArray(raw)) {
    const joined = raw.map((item) => item?.msg || JSON.stringify(item)).join(', ');
    return joined || fallback;
  }
  if (raw && typeof raw === 'object') return JSON.stringify(raw);
  return raw || fallback;
}
