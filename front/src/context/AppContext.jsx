import { createContext, useContext, useState, useEffect, useCallback } from 'react';

const AppContext = createContext(null);

export function AppProvider({ children }) {
  const [token, setToken] = useState(null);
  const [workspaceInfo, setWorkspaceInfo] = useState(null);
  const [projectInfo, setProjectInfo] = useState(null);
  const [ready, setReady] = useState(false);

  const handleMessage = useCallback((event) => {
    const data = event.data;
    if (!data || typeof data !== 'object' || !data.accessToken) return;
    setToken(data.accessToken);
    setWorkspaceInfo(data.workspaceInfo || null);
    setProjectInfo(data.projectInfo || null);
    setReady(true);
  }, []);

  useEffect(() => {
    window.addEventListener('message', handleMessage);

    // Allow standalone usage with URL params for dev/testing
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get('token');
    if (urlToken) {
      setToken(urlToken);
      setProjectInfo({ ns_id: params.get('ns_id') || '' });
      setReady(true);
    }

    return () => window.removeEventListener('message', handleMessage);
  }, [handleMessage]);

  const nsId = projectInfo?.ns_id || '';

  return (
    <AppContext.Provider value={{ token, workspaceInfo, projectInfo, nsId, ready }}>
      {children}
    </AppContext.Provider>
  );
}

export function useAppContext() {
  return useContext(AppContext);
}
