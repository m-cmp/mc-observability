import { Outlet } from 'react-router-dom';
import useFollowParentNamespace from '../hooks/useFollowParentNamespace';

export default function EmbedLayout() {
  useFollowParentNamespace(); // follow parent-page namespace changes for the whole session
  return (
    <div className="h-screen overflow-auto p-4">
      <Outlet />
    </div>
  );
}
