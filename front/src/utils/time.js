// Format an ISO/epoch timestamp in the browser's local timezone (YYYY-MM-DD HH:mm:ss).
//
// Backend LocalDateTime values arrive as a naive ISO string with no timezone
// (e.g. "2026-06-12T10:20:38.528956"), but they are UTC. `new Date()` would treat
// such a string as *local* time and skip the conversion, so we mark naive datetime
// strings as UTC (append "Z") before parsing. Epoch numbers and timezone-aware
// strings (with "Z" or an offset) are passed through unchanged.
const NAIVE_DATETIME = /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}(:\d{2})?(\.\d+)?$/;

export function formatLocalTime(value) {
  if (value == null || value === '') return '';
  let v = value;
  if (typeof v === 'string' && NAIVE_DATETIME.test(v.trim())) {
    v = v.trim().replace(' ', 'T') + 'Z';
  }
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return String(value);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
