import { useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { useAppContext } from './context/AppContext';
import { setApiToken } from './api/client';
import Layout from './components/Layout';
import ErrorBoundary from './components/ErrorBoundary';
import EmbedLayout from './components/EmbedLayout';
import MonitoringDashboard from './pages/MonitoringDashboard';
import MciOverview from './pages/MciOverview';
import LogViewer from './pages/LogViewer';
import MonitoringConfig from './pages/MonitoringConfig';
import InsightDashboard from './pages/InsightDashboard';
import AlertManager from './pages/AlertManager';
import K8sNodeDashboard from './pages/K8sNodeDashboard';
import HomePage from './pages/HomePage';

export default function App() {
  const { token } = useAppContext();

  useEffect(() => {
    if (token) setApiToken(token);
  }, [token]);

  return (
    <ErrorBoundary>
    <Routes>
      {/* Standalone — full nav + test console */}
      <Route path="/" element={<HomePage />} />
      <Route element={<Layout />}>
        <Route path="/monitoring/:nsId/k8s/:connectionName/:clusterName/:nodeGroupName/:nodeNumber" element={<K8sNodeDashboard />} />
        <Route path="/monitoring/:nsId/:mciId/:vmId" element={<MonitoringDashboard />} />
        <Route path="/monitoring/:nsId/:mciId" element={<MciOverview />} />
        <Route path="/monitoring/:nsId" element={<MciOverview />} />
        <Route path="/logs/:nsId/:mciId/:vmId" element={<LogViewer />} />
        <Route path="/logs/:nsId/:mciId" element={<LogViewer />} />
        <Route path="/logs/:nsId" element={<LogViewer />} />
        <Route path="/config/:nsId/:mciId" element={<MonitoringConfig />} />
        <Route path="/config/:nsId" element={<MonitoringConfig />} />
        <Route path="/insight/:nsId/:mciId/:vmId" element={<InsightDashboard />} />
        <Route path="/insight/:nsId/:mciId" element={<InsightDashboard />} />
        <Route path="/insight/:nsId" element={<InsightDashboard />} />
        <Route path="/alerts/:nsId/:mciId" element={<AlertManager />} />
        <Route path="/alerts/:nsId" element={<AlertManager />} />
      </Route>

      {/* Embed — no nav, for iframe */}
      <Route element={<EmbedLayout />}>
        <Route path="/embed/monitoring/:nsId/k8s/:connectionName/:clusterName/:nodeGroupName/:nodeNumber" element={<K8sNodeDashboard />} />
        <Route path="/embed/monitoring/:nsId/:mciId/:vmId" element={<MonitoringDashboard />} />
        <Route path="/embed/monitoring/:nsId/:mciId" element={<MciOverview />} />
        <Route path="/embed/monitoring/:nsId" element={<MciOverview />} />
        <Route path="/embed/logs/:nsId/:mciId/:vmId" element={<LogViewer />} />
        <Route path="/embed/logs/:nsId/:mciId" element={<LogViewer />} />
        <Route path="/embed/logs/:nsId" element={<LogViewer />} />
        <Route path="/embed/config/:nsId/:mciId" element={<MonitoringConfig />} />
        <Route path="/embed/config/:nsId" element={<MonitoringConfig />} />
        <Route path="/embed/insight/:nsId/:mciId/:vmId" element={<InsightDashboard />} />
        <Route path="/embed/insight/:nsId/:mciId" element={<InsightDashboard />} />
        <Route path="/embed/insight/:nsId" element={<InsightDashboard />} />
        <Route path="/embed/alerts/:nsId/:mciId" element={<AlertManager />} />
        <Route path="/embed/alerts/:nsId" element={<AlertManager />} />
      </Route>
    </Routes>
    </ErrorBoundary>
  );
}
