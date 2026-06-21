import { useNavigate } from 'react-router-dom';
import useBasePath from '../hooks/useBasePath';
import { nodeRunState, nodeStateMessage } from '../utils/nodeState';

/**
 * Shown instead of an empty "No data" state for a node with no metrics. The message depends on the
 * node run-state:
 *  - running  → agent genuinely not installed → guide to Config to install.
 *  - stopped  → node not running → nothing to collect; no install CTA.
 *  - unknown  → status undefined/unknown → "agent state unknown"; no install CTA.
 *
 * Props: nsId, infraId, nodeId (optional), nodeStatus, height, compact
 */
export default function AgentNotInstalled({ nsId, infraId, nodeId, nodeStatus, height = 220, compact = false }) {
  const navigate = useNavigate();
  const base = useBasePath();
  const run = nodeRunState(nodeStatus);

  const goConfig = () => {
    if (!nsId) return;
    let path = `${base}/config/${nsId}`;
    if (infraId) path += `/${infraId}`;
    navigate(path);
  };

  return (
    <div
      className="flex flex-col items-center justify-center text-center gap-2 text-gray-500"
      style={{ minHeight: height }}
    >
      <svg className="w-8 h-8 text-gray-300" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth="1.5">
        <path strokeLinecap="round" strokeLinejoin="round" d="M12 9v3.75m0 3.75h.008M10.34 3.94l-7.5 12.99A1.5 1.5 0 004.14 19.5h15.72a1.5 1.5 0 001.3-2.57l-7.5-12.99a1.5 1.5 0 00-2.62 0z" />
      </svg>
      {run === 'running' ? (
        <>
          <p className="text-sm font-medium text-gray-600">Monitoring agent is not installed.</p>
          {!compact && (
            <p className="text-xs text-gray-400">
              Install the agent from the Config menu to start collecting metrics.
            </p>
          )}
          {nsId && (
            <button
              onClick={goConfig}
              className="mt-1 px-3 py-1.5 text-sm bg-blue-600 text-white rounded hover:bg-blue-700"
            >
              Go to Config to install agent
            </button>
          )}
        </>
      ) : (
        <p className="text-sm font-medium text-gray-600">{nodeStateMessage(nodeStatus)}</p>
      )}
    </div>
  );
}
