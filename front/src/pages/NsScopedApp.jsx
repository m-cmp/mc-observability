import { useParams, useNavigate } from 'react-router-dom';
import useBasePath from '../hooks/useBasePath';
import useFollowParentNamespace from '../hooks/useFollowParentNamespace';
import InfraOverview from './InfraOverview';
import MonitoringDashboard from './MonitoringDashboard';

// Idle = black text (no color). Active = per-feature colored background + white
// text. Explicit class strings so Tailwind keeps them.
const IDLE = 'text-gray-800 hover:bg-gray-100';
const navItems = [
  { label: 'Monitoring', key: 'monitoring', active: 'bg-blue-600 text-white' },
  { label: 'Logs', key: 'logs', active: 'bg-emerald-600 text-white' },
  { label: 'Config', key: 'config', active: 'bg-amber-600 text-white' },
  { label: 'Insight', key: 'insight', active: 'bg-purple-600 text-white' },
  { label: 'Alerts', key: 'alerts', active: 'bg-red-600 text-white' },
  { label: 'Tracing', key: 'trace', active: 'bg-teal-600 text-white' },
];

/**
 * Namespace-scoped iframe entry used by `/:nsId/:infraId` (Infra level) and
 * `/:nsId/:infraId/:nodeId` (Node level).
 *
 * - No product logo, no NS/Infra selectors (ns + infra are in the path).
 * - Shows the Monitoring view of the embedded target (Infra overview / Node
 *   dashboard) — Monitoring is the landing section.
 * - The top menu keeps the current infra/node scope, navigating to
 *   `/{section}/{ns}/{infra}[/{node}]` so the target context follows the user
 *   (e.g. Insight opens scoped to the same infra/node).
 * - Node level: a Back button returns to the Infra level.
 */
export default function NsScopedApp() {
  const { nsId, infraId, nodeId } = useParams();
  const navigate = useNavigate();
  const base = useBasePath();
  useFollowParentNamespace(); // follow parent-page namespace changes for the whole session

  return (
    <div className="flex flex-col h-screen">
      <nav className="flex items-center gap-1 px-4 py-2 bg-white border-b border-gray-200 text-sm shrink-0 flex-wrap">
        {/* Node level → Back button (replaces the logo area) */}
        {nodeId && (
          <button
            onClick={() => navigate(`${base}/${nsId}/${infraId}`)}
            className="mr-2 px-2.5 py-1.5 rounded-md text-gray-600 hover:bg-gray-100 inline-flex items-center gap-1"
            title="Back to Infra"
          >
            <span aria-hidden>←</span> Back
          </button>
        )}

        {navItems.map((item) => {
          // Config has no node-level route; cap it at the infra level.
          const suffix = nodeId && item.key !== 'config' ? `/${nodeId}` : '';
          return (
            <button
              key={item.key}
              onClick={() => navigate(`${base}/${item.key}/${nsId}/${infraId}${suffix}`)}
              className={`px-3 py-1.5 rounded-md transition-colors ${
                item.key === 'monitoring' ? item.active : IDLE
              }`}
            >
              {item.label}
            </button>
          );
        })}
      </nav>

      <main className="flex-1 overflow-auto p-4 bg-gray-50">
        {nodeId ? <MonitoringDashboard /> : <InfraOverview />}
      </main>
    </div>
  );
}
