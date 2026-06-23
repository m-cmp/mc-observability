// Tiny client-side cache for hot, semi-static list lookups (VM/infra lists, k8s clusters).
//
// Two jobs:
//  1. TTL cache  — repeated reads within `ttlMs` reuse the result instead of re-hitting
//     cb-tumblebug, so tab switches / multiple panels don't each pay a network round-trip.
//  2. In-flight dedup — concurrent identical requests share one promise, so a burst (e.g. the
//     landing page counting several namespaces, or many cards mounting at once) collapses to a
//     single call. This is what keeps us under cb-tumblebug's rate limiter (HTTP 429).
//
// Roughly an LRU of the most-recently-used entries (cap below), so the recently-viewed
// VMs/clusters stay warm.

const MAX_ENTRIES = 60;
const store = new Map(); // key -> { ts, value }
const inflight = new Map(); // key -> Promise

function trim() {
  if (store.size <= MAX_ENTRIES) return;
  const oldest = [...store.entries()].sort((a, b) => a[1].ts - b[1].ts);
  for (let i = 0; i < oldest.length - MAX_ENTRIES; i++) store.delete(oldest[i][0]);
}

export function cached(key, ttlMs, loader) {
  const hit = store.get(key);
  if (hit && Date.now() - hit.ts < ttlMs) return Promise.resolve(hit.value);
  if (inflight.has(key)) return inflight.get(key);
  const p = loader()
    .then((value) => {
      store.set(key, { ts: Date.now(), value });
      inflight.delete(key);
      trim();
      return value;
    })
    .catch((e) => {
      inflight.delete(key);
      throw e;
    });
  inflight.set(key, p);
  return p;
}

/** Drop cached entries whose key starts with `prefix` (e.g. after an install/uninstall). */
export function invalidate(prefix = '') {
  for (const k of [...store.keys()]) if (k.startsWith(prefix)) store.delete(k);
}
