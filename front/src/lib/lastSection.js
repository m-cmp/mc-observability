// Remembers the last section (Monitoring/Logs/Config/…) the user was on, so that when the
// embedding console switches namespace — which reloads this iframe from scratch — we can land
// on the same section under the new namespace instead of always snapping back to Monitoring.
// Persisted in localStorage (same :18081 origin across reloads).
const KEY = 'o11y:lastSection';
export const SECTIONS = ['monitoring', 'logs', 'config', 'insight', 'alerts', 'trace'];

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
