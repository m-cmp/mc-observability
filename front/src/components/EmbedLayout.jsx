import { Outlet } from 'react-router-dom';

export default function EmbedLayout() {
  return (
    <div className="h-screen overflow-auto p-4">
      <Outlet />
    </div>
  );
}
