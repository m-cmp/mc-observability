import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getNsList, getInfraList } from '../api/tumblebug';
import { getK8sClusters } from '../api/k8sAgent';
import useParentNamespace from '../hooks/useParentNamespace';

/**
 * Landing shown at `/` when no namespace is passed in the path.
 * Lets the user pick a namespace; selecting one navigates to `/:nsId`,
 * which renders the full menu-driven app scoped to that namespace.
 * Designed so an iframe can point at `/` (no ns) and still drive everything.
 */
export default function NamespaceHome() {
  const navigate = useNavigate();
  const parentNs = useParentNamespace(); // namespace from the embedding page (if any)
  const [nsList, setNsList] = useState([]);
  const [counts, setCounts] = useState({}); // ns -> { infra, k8s }
  const [loading, setLoading] = useState(true);
  const [counting, setCounting] = useState(false); // infra/cluster counts being aggregated

  // If the parent page exposes a selected project, reflect it automatically:
  // resolve the displayed text against the ns list (by id or name) and open it.
  // When it can't be read, parentNs is '' and we just show the picker below.
  useEffect(() => {
    if (!parentNs || loading) return;
    const match = nsList.find((n) => n.id === parentNs || (n.name && n.name === parentNs));
    navigate(`/${match ? match.id : parentNs}`, { replace: true });
  }, [parentNs, loading, nsList, navigate]);

  useEffect(() => {
    let alive = true;
    setLoading(true);
    getNsList()
      .then(async (list) => {
        const arr = Array.isArray(list) ? list : [];
        if (alive) { setNsList(arr); setLoading(false); setCounting(true); }
        // best-effort infra + k8s cluster counts per namespace
        const entries = await Promise.all(
          arr.map(async (ns) => {
            const [inf, cl] = await Promise.allSettled([getInfraList(ns.id), getK8sClusters(ns.id)]);
            return [ns.id, {
              infra: inf.status === 'fulfilled' && Array.isArray(inf.value) ? inf.value.length : null,
              k8s: cl.status === 'fulfilled' && Array.isArray(cl.value) ? cl.value.length : null,
            }];
          })
        );
        if (alive) { setCounts(Object.fromEntries(entries)); setCounting(false); }
      })
      .catch(() => { if (alive) { setNsList([]); setLoading(false); setCounting(false); } });
    return () => { alive = false; };
  }, []);

  return (
    <div className="max-w-3xl mx-auto">
      <div className="mb-6">
        <h1 className="text-lg font-bold text-gray-800">Select a Namespace</h1>
        <p className="text-sm text-gray-500 mt-1">
          Choose a namespace to open its monitoring, logs, config, insight, alerts and tracing.
        </p>
      </div>

      {loading ? (
        <div className="text-sm text-gray-400">Loading namespaces…</div>
      ) : nsList.length === 0 ? (
        <div className="text-sm text-gray-400">No namespaces found.</div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {nsList.map((ns) => (
            <button
              key={ns.id}
              onClick={() => navigate(`/${ns.id}`)}
              className="text-left bg-white border border-gray-200 rounded-lg p-4 hover:border-blue-500 hover:shadow-sm transition-colors"
            >
              <div className="font-semibold text-gray-800 break-all">{ns.name || ns.id}</div>
              {ns.name && ns.name !== ns.id && (
                <div className="text-xs text-gray-400 break-all">{ns.id}</div>
              )}
              <div className="mt-2 text-xs text-gray-500">
                {counts[ns.id] == null ? (
                  counting ? <span className="text-gray-400 animate-pulse">counting…</span> : '—'
                ) : (
                  `${counts[ns.id].infra ?? '—'} infra · ${counts[ns.id].k8s ?? '—'} k8s`
                )}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
