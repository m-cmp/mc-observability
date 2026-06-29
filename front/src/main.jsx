import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AppProvider } from './context/AppContext';
import './index.css';

// Capture the embedding console's selected namespace as early as possible. The parent
// posts it via postMessage on iframe load, which can arrive before React mounts the
// listener in useParentNamespace — stash it on window so the hook can seed from it.
if (typeof window !== 'undefined') {
  window.addEventListener('message', (e) => {
    const d = e && e.data;
    if (!d || typeof d !== 'object') return;
    const p = d.projectInfo || d.project || {};
    const ns = p.ns_id || p.nsId || p.NsId || d.ns_id || d.nsId || d.NsId;
    if (ns) window.__parentNs = String(ns).trim();
  });
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AppProvider>
        <App />
      </AppProvider>
    </BrowserRouter>
  </React.StrictMode>
);
