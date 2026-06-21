// Classify a Tumblebug node status for agent-related UI decisions.
//
// - running:  the VM is up; "agent not installed" is meaningful → show install guidance.
// - stopped:  the VM is suspended/terminated/etc; we can't know agent state and there's
//             nothing to collect → show a state-specific message, not "install".
// - unknown:  status missing/undefined → "agent state unknown".

export function nodeRunState(status) {
  const s = (status || '').toString().toLowerCase();
  if (!s || s.includes('undefined')) return 'unknown';
  if (s.includes('running')) return 'running';
  if (
    s.includes('suspend') || s.includes('terminat') || s.includes('stop') ||
    s.includes('failed') || s.includes('creating') || s.includes('reboot')
  ) {
    return 'stopped';
  }
  return 'unknown';
}

/** Human label for a non-running state (used when agent install is not applicable). */
export function nodeStateMessage(status) {
  const s = (status || '').toString();
  const run = nodeRunState(status);
  if (run === 'unknown') {
    return `Agent state unknown (node status: ${s || 'N/A'}).`;
  }
  // stopped
  return `Node is not running (${s}). Start the node to collect metrics.`;
}
