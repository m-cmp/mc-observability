// Remembers the last section (Monitoring/Logs/Config/…) the user was on, so that when the
// embedding console switches namespace — which reloads this iframe from scratch — we can land
// on the same section under the new namespace instead of always snapping back to Monitoring.
// Persisted in localStorage (same :18081 origin across reloads).
const KEY = 'o11y:lastSection';
export const SECTIONS = ['monitoring', 'logs', 'config', 'insight', 'alerts', 'trace'];

/**
 * Canonical section name for a path segment, or '' when it names no section.
 *
 * React Router matches static path segments case-insensitively, so `/embed/LOGS/{ns}` already
 * reaches the Logs route. Matching this list case-sensitively made the section entry points
 * disagree with that: `/embed/Logs` fell through to NotFound while `/embed/LOGS/{ns}` rendered
 * fine. Normalising here keeps both halves of the URL space on the same rule.
 */
export function toSection(segment) {
  if (!segment) return '';
  const s = String(segment).toLowerCase();
  return SECTIONS.includes(s) ? s : '';
}

export function setLastSection(section) {
  try {
    if (SECTIONS.includes(section)) localStorage.setItem(KEY, section);
  } catch {
    /* private mode / storage disabled — ignore */
  }
}

export function getLastSection() {
  try {
    const s = localStorage.getItem(KEY);
    return SECTIONS.includes(s) ? s : '';
  } catch {
    return '';
  }
}
