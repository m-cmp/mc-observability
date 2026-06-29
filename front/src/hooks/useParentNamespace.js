import { useState, useEffect } from 'react';

// The embedding (parent) console tells us which namespace is selected in two ways:
//
//  1. postMessage — the parent posts a payload to the iframe on load / project change
//     (mc-web-console's iframe.js does `iframe.contentWindow.postMessage(data, src)`).
//     This is the ONLY reliable channel when the parent is a different origin — e.g. the
//     console and this app on :18081 are cross-origin (different port at least), so the
//     same-origin policy blocks reading `window.parent.document` entirely.
//
//  2. a `#select-current-project` <select> in the parent DOM, whose selected <option>
//     carries the namespace in `data-nsid` (falling back to the displayed text). Only
//     readable when the parent is same-origin (e.g. local dev), so it's just a fallback.
//
// We prefer the postMessage value and fall back to the DOM read.
const SELECT_ID = 'select-current-project';

function readParentNamespaceFromDom() {
  try {
    if (typeof window === 'undefined' || window.parent === window) return '';
    const doc = window.parent.document; // throws on cross-origin → caught below
    const sel = doc.getElementById(SELECT_ID);
    if (!sel) return '';
    const opt = sel.options && sel.selectedIndex >= 0 ? sel.options[sel.selectedIndex] : null;
    if (!opt) return '';
    // The namespace id lives in data-nsid; the visible text is only a label.
    const nsid = opt.getAttribute ? opt.getAttribute('data-nsid') : '';
    const text = opt.text ?? opt.label ?? opt.textContent ?? '';
    return String(nsid || text).trim();
  } catch {
    return ''; // cross-origin or any access issue → just skip, not an error
  }
}

// Extract a namespace id from a parent postMessage payload. The console sends
// `{ projectInfo: { id, ns_id, name }, ... }`; accept a few spellings/locations.
function nsFromMessage(data) {
  if (!data || typeof data !== 'object') return '';
  const p = data.projectInfo || data.project || {};
  const candidates = [
    p.ns_id, p.nsId, p.NsId, p.nsid,
    data.ns_id, data.nsId, data.NsId,
  ];
  for (const c of candidates) {
    if (c) return String(c).trim();
  }
  return '';
}

// Seed from an already-captured parent message (see main.jsx) before falling back to
// the same-origin DOM read — avoids missing the parent's one-shot on-load postMessage.
function initialNs() {
  if (typeof window !== 'undefined' && window.__parentNs) return window.__parentNs;
  return readParentNamespaceFromDom();
}

/**
 * Returns the namespace selected in the embedding console. Listens for the parent's
 * postMessage (cross-origin friendly) and, as a same-origin fallback, watches the
 * parent's `#select-current-project`. Returns '' when nothing can be determined.
 */
export default function useParentNamespace() {
  const [ns, setNs] = useState(initialNs);

  useEffect(() => {
    // 1. postMessage — works across origins (the real integration path).
    const onMessage = (e) => {
      const v = nsFromMessage(e.data);
      if (v) setNs(v);
    };
    window.addEventListener('message', onMessage);

    // 2. window.__parentNs (captured by the early listener in main.jsx) + same-origin DOM read,
    //    polled. This recovers the parent's one-shot on-load postMessage even if it landed in the
    //    gap between this component's first render and this effect attaching its own listener —
    //    main.jsx's module-level listener still caught it into window.__parentNs.
    let sel = null;
    const update = () => {
      const v = (typeof window !== 'undefined' && window.__parentNs) || readParentNamespaceFromDom();
      if (v) setNs(v);
    };
    try {
      if (window.parent !== window) {
        sel = window.parent.document.getElementById(SELECT_ID);
        if (sel) sel.addEventListener('change', update);
      }
    } catch {
      sel = null; // cross-origin: cannot attach listener, postMessage covers it
    }
    // Read once immediately: the parent's on-load postMessage may have already landed in
    // window.__parentNs (via main.jsx) before this effect attached its listener, so don't wait
    // for the first poll tick — pick it up now for an instant switch.
    update();
    const timer = setInterval(update, 1500);

    return () => {
      window.removeEventListener('message', onMessage);
      if (sel) { try { sel.removeEventListener('change', update); } catch { /* ignore */ } }
      clearInterval(timer);
    };
  }, []);

  return ns;
}
