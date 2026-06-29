import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import useParentNamespace from './useParentNamespace';
import useBasePath from './useBasePath';
import { getNsList } from '../api/tumblebug';
import { getLastSection, setLastSection, SECTIONS } from '../lib/lastSection';

// Pull the section (if any) and the namespace out of the current path, ignoring the
// optional /embed base. Paths look like `/{ns}`, `/{ns}/{infra}[/{node}]`, or
// `/{section}/{ns}/...`.
function parsePath(pathname, base) {
  let p = pathname;
  if (base && p.startsWith(base)) p = p.slice(base.length);
  const segs = p.split('/').filter(Boolean);
  if (segs.length && SECTIONS.includes(segs[0])) return { section: segs[0], ns: segs[1] || '' };
  return { section: '', ns: segs[0] || '' };
}

/**
 * Keeps the embedded app's namespace in sync with the parent page's project selector for the
 * WHOLE session — not just on first load. When the parent switches namespace, navigate the
 * iframe to the same section under the new namespace (infra/node context is dropped since it's
 * namespace-specific). Mount once in each top-level shell.
 */
export default function useFollowParentNamespace() {
  const parentNs = useParentNamespace();
  const navigate = useNavigate();
  const base = useBasePath();
  const { pathname } = useLocation();

  // Remember the section the user is on so a parent namespace switch (which reloads this
  // iframe) can return to the same menu instead of snapping back to Monitoring.
  useEffect(() => {
    const { section } = parsePath(pathname, base);
    if (section) setLastSection(section);
  }, [pathname, base]);

  useEffect(() => {
    if (!parentNs) return;
    let alive = true;
    const { section, ns: currentNs } = parsePath(pathname, base);
    // Resolve the parent's displayed text (could be an id or a name) to a namespace id.
    getNsList()
      .then((list) => {
        if (!alive) return;
        const match = (list || []).find((n) => n.id === parentNs || n.name === parentNs);
        const targetNs = match ? match.id : parentNs;
        if (targetNs && targetNs !== currentNs) {
          // Preserve the current section; at the namespace root (no section) fall back to the
          // last remembered section (then Monitoring) so the chosen menu survives a switch.
          const sec = section || getLastSection() || 'monitoring';
          navigate(`${base}/${sec}/${targetNs}`, { replace: true });
        }
      })
      .catch(() => {});
    return () => { alive = false; };
  }, [parentNs, pathname, base, navigate]);
}
