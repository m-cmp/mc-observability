import { Link, useLocation } from 'react-router-dom';
import useBasePath from '../hooks/useBasePath';

/**
 * Catch-all. Without it an unmatched path renders nothing at all, which inside an iframe is a
 * blank panel with no clue as to why - the failure mode most likely to show up while the
 * embedding console is being wired to new per-section sub-menu links.
 */
export default function NotFound() {
  const { pathname } = useLocation();
  const base = useBasePath();
  return (
    <div className="p-6 text-sm">
      <div className="font-semibold text-gray-800">No page for this address</div>
      <div className="mt-1 text-gray-500 break-all">{pathname}</div>
      <p className="mt-3 text-gray-500">
        Expected <code className="px-1 bg-gray-100 rounded">{base || ''}/&#123;section&#125;/&#123;namespace&#125;</code>,
        where section is one of monitoring, logs, config, insight, alerts, trace.
      </p>
      <Link to={`${base}/`} className="inline-block mt-3 text-blue-600 hover:underline">
        Pick a namespace
      </Link>
    </div>
  );
}
