import { useEffect } from 'react';
import { Routes, Route } from 'react-router-dom';
import { useAppContext } from './context/AppContext';
import { setApiToken } from './api/client';
import Layout from './components/Layout';
import ErrorBoundary from './components/ErrorBoundary';
import EmbedLayout from './components/EmbedLayout';
import MonitoringDashboard from './pages/MonitoringDashboard';
import InfraOverview from './pages/InfraOverview';
import LogViewer from './pages/LogViewer';
import MonitoringConfig from './pages/MonitoringConfig';
import InsightDashboard from './pages/InsightDashboard';
import AlertManager from './pages/AlertManager';
import TraceViewer from './pages/TraceViewer';
import K8sNodeDashboard from './pages/K8sNodeDashboard';
import HomePage from './pages/HomePage';
import NamespaceHome from './pages/NamespaceHome';
import NsScopedApp from './pages/NsScopedApp';

export default function App() {
  const { token } = useAppContext();

  useEffect(() => {
    if (token) setApiToken(token);
  }, [token]);

  return (
    <ErrorBoundary>
    <Routes>
      {/* Dev/test console (was at "/") */}
      <Route path="/console" element={<HomePage />} />

      {/* Namespace-scoped iframe app: no logo, in-place section switching.
          Infra level → Infra selector only; Node level → no selectors + Back. */}
      <Route path="/:nsId/:infraId" element={<NsScopedApp />} />
      <Route path="/:nsId/:infraId/:nodeId" element={<NsScopedApp />} />

      {/* Standalone — full nav + NS/Infra selectors */}
      <Route element={<Layout />}>
        {/* Namespace-rooted entry: `/` shows NS picker, `/:nsId` shows the
            namespace overview with full nav (NS selector). */}
        <Route path="/" element={<NamespaceHome />} />
        <Route path="/:nsId" element={<InfraOverview />} />
        <Route path="/monitoring/:nsId/k8s/:connectionName/:clusterName/:nodeGroupName/:nodeNumber" element={<K8sNodeDashboard />} />
        <Route path="/monitoring/:nsId/:infraId/:nodeId" element={<MonitoringDashboard />} />
        <Route path="/monitoring/:nsId/:infraId" element={<InfraOverview />} />
        <Route path="/monitoring/:nsId" element={<InfraOverview />} />
        <Route path="/logs/:nsId/:infraId/:nodeId" element={<LogViewer />} />
        <Route path="/logs/:nsId/:infraId" element={<LogViewer />} />
        <Route path="/logs/:nsId" element={<LogViewer />} />
        <Route path="/config/:nsId/:infraId" element={<MonitoringConfig />} />
        <Route path="/config/:nsId" element={<MonitoringConfig />} />
        <Route path="/insight/:nsId/:infraId/:nodeId" element={<InsightDashboard />} />
        <Route path="/insight/:nsId/:infraId" element={<InsightDashboard />} />
        <Route path="/insight/:nsId" element={<InsightDashboard />} />
        <Route path="/alerts/:nsId/:infraId/:nodeId" element={<AlertManager />} />
        <Route path="/alerts/:nsId/:infraId" element={<AlertManager />} />
        <Route path="/alerts/:nsId" element={<AlertManager />} />
        <Route path="/trace/:nsId/:infraId/:nodeId" element={<TraceViewer />} />
        <Route path="/trace/:nsId/:infraId" element={<TraceViewer />} />
        <Route path="/trace/:nsId" element={<TraceViewer />} />
      </Route>

      {/* Embed — no nav, for iframe */}
      <Route element={<EmbedLayout />}>
        <Route path="/embed/monitoring/:nsId/k8s/:connectionName/:clusterName/:nodeGroupName/:nodeNumber" element={<K8sNodeDashboard />} />
        <Route path="/embed/monitoring/:nsId/:infraId/:nodeId" element={<MonitoringDashboard />} />
        <Route path="/embed/monitoring/:nsId/:infraId" element={<InfraOverview />} />
        <Route path="/embed/monitoring/:nsId" element={<InfraOverview />} />
        <Route path="/embed/logs/:nsId/:infraId/:nodeId" element={<LogViewer />} />
        <Route path="/embed/logs/:nsId/:infraId" element={<LogViewer />} />
        <Route path="/embed/logs/:nsId" element={<LogViewer />} />
        <Route path="/embed/config/:nsId/:infraId" element={<MonitoringConfig />} />
        <Route path="/embed/config/:nsId" element={<MonitoringConfig />} />
        <Route path="/embed/insight/:nsId/:infraId/:nodeId" element={<InsightDashboard />} />
        <Route path="/embed/insight/:nsId/:infraId" element={<InsightDashboard />} />
        <Route path="/embed/insight/:nsId" element={<InsightDashboard />} />
        <Route path="/embed/alerts/:nsId/:infraId/:nodeId" element={<AlertManager />} />
        <Route path="/embed/alerts/:nsId/:infraId" element={<AlertManager />} />
        <Route path="/embed/alerts/:nsId" element={<AlertManager />} />
        <Route path="/embed/trace/:nsId/:infraId/:nodeId" element={<TraceViewer />} />
        <Route path="/embed/trace/:nsId/:infraId" element={<TraceViewer />} />
        <Route path="/embed/trace/:nsId" element={<TraceViewer />} />
      </Route>
    </Routes>
    </ErrorBoundary>
  );
}
