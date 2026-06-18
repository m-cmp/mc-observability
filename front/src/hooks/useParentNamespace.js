import { useState, useEffect } from 'react';

// The embedding (parent) page is expected to have a project selector with this id.
// We read the *displayed text* of the selected option (not its value) and use it
// as the namespace. If the parent DOM cannot be read (cross-origin, not embedded,
// element missing), we silently return '' — no error — so the app falls back to
// the manual namespace picker.
const SELECT_ID = 'select-current-project';

function readParentNamespace() {
  try {
    if (typeof window === 'undefined' || window.parent === window) return '';
    const doc = window.parent.document; // throws on cross-origin → caught below
    const sel = doc.getElementById(SELECT_ID);
    if (!sel) return '';
    const opt = sel.options && sel.selectedIndex >= 0 ? sel.options[sel.selectedIndex] : null;
    const text = opt ? (opt.text ?? opt.label ?? opt.textContent ?? '') : '';
    return String(text).trim();
  } catch {
    return ''; // cross-origin or any access issue → just skip, not an error
  }
}

/**
 * Watches the parent page's `#select-current-project` selected option text and
 * returns it as the namespace. Returns '' when it cannot be read.
 */
export default function useParentNamespace() {
  const [ns, setNs] = useState(readParentNamespace);

  useEffect(() => {
    let sel = null;
    const update = () => setNs(readParentNamespace());
    try {
      if (window.parent !== window) {
        sel = window.parent.document.getElementById(SELECT_ID);
        if (sel) sel.addEventListener('change', update);
      }
    } catch {
      sel = null; // cross-origin: cannot attach listener, fall back to polling only
    }
    // Poll as a fallback in case the parent updates the selection without a
    // reachable change event. Cheap DOM read; stops on unmount.
    const timer = setInterval(update, 1500);
    return () => {
      if (sel) { try { sel.removeEventListener('change', update); } catch { /* ignore */ } }
      clearInterval(timer);
    };
  }, []);

  return ns;
}
