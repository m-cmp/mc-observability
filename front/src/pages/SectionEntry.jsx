import { useParams } from 'react-router-dom';
import { toSection } from '../lib/lastSection';
import NamespaceHome from './NamespaceHome';
import InfraOverview from './InfraOverview';
import NotFound from './NotFound';

/**
 * Entry for a section link that carries no namespace, e.g. `/embed/logs`.
 *
 * The embedding console builds its sub-menu links statically, so requiring the namespace in
 * the iframe src would force the console to rebuild that src on every project change. Pointing
 * a sub-menu here instead lets the URL name the section while the namespace keeps arriving over
 * postMessage, exactly as it already does today. Until one shows up we render the namespace
 * picker pinned to this section, so a standalone visit still works.
 */
export default function SectionEntry() {
  const { section } = useParams();
  const canonical = toSection(section);
  if (!canonical) return <NotFound />;
  return <NamespaceHome section={canonical} />;
}

/**
 * `/:nsId` and `/{section}` are both a single segment, so one route serves both: treat the
 * segment as a section when it names one, and as a namespace otherwise. Namespace ids that
 * collide with a section name are unreachable here, which is why the embed routes - the ones
 * the console actually loads - keep the two apart under an explicit `/embed/{section}` path.
 */
export function NsRootOrSection() {
  const { nsId } = useParams();
  const section = toSection(nsId);
  return section ? <NamespaceHome section={section} /> : <InfraOverview />;
}
