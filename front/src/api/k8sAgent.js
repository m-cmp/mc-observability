import client from './client';
import { cached } from './cache';

// K8s clusters in a namespace (from cb-tumblebug, proxied by nginx). Cached ~30s with
// in-flight dedup since several panels list clusters and the lookup is heavy.
export async function getK8sClusters(nsId) {
  return cached(`k8sClusters:${nsId}`, 30000, async () => {
    const res = await client.get(`/tumblebug/ns/${nsId}/k8sCluster`);
    return res.data?.K8sClusterInfo || res.data?.k8sClusterInfo || [];
  });
}

// Host Telegraf agent on K8s cluster nodes (mc-observability manager).
export async function getK8sAgentStatus(nsId, clusterId) {
  const res = await client.get(`/api/o11y/monitoring/k8s/${nsId}/${clusterId}/agent`);
  return res.data?.data || [];
}

export async function installK8sAgent(nsId, clusterId) {
  const res = await client.post(`/api/o11y/monitoring/k8s/${nsId}/${clusterId}/agent`);
  return res.data?.data || [];
}

export async function uninstallK8sAgent(nsId, clusterId) {
  const res = await client.delete(`/api/o11y/monitoring/k8s/${nsId}/${clusterId}/agent`);
  return res.data?.data || [];
}

// Per-node operations (like VM nodes).
export async function installK8sNode(nsId, clusterId, nodeName, metrics) {
  const res = await client.post(
    `/api/o11y/monitoring/k8s/${nsId}/${clusterId}/node/${encodeURIComponent(nodeName)}/agent`,
    metrics ? { metrics } : {}
  );
  return res.data?.data;
}

export async function uninstallK8sNode(nsId, clusterId, nodeName) {
  const res = await client.delete(
    `/api/o11y/monitoring/k8s/${nsId}/${clusterId}/node/${encodeURIComponent(nodeName)}/agent`
  );
  return res.data?.data;
}

export async function getK8sNodeMetrics(nsId, clusterId, nodeName) {
  const res = await client.get(
    `/api/o11y/monitoring/k8s/${nsId}/${clusterId}/node/${encodeURIComponent(nodeName)}/agent/metrics`
  );
  return res.data?.data || { available: [], active: [] };
}

// Log agent (fluent-bit via podman) on K8s cluster nodes.
export async function getK8sLogStatus(nsId, clusterId) {
  const res = await client.get(`/api/o11y/monitoring/k8s/${nsId}/${clusterId}/log-agent`);
  return res.data?.data || [];
}

export async function installK8sLogNode(nsId, clusterId, nodeName) {
  const res = await client.post(
    `/api/o11y/monitoring/k8s/${nsId}/${clusterId}/node/${encodeURIComponent(nodeName)}/log-agent`
  );
  return res.data?.data;
}

export async function uninstallK8sLogNode(nsId, clusterId, nodeName) {
  const res = await client.delete(
    `/api/o11y/monitoring/k8s/${nsId}/${clusterId}/node/${encodeURIComponent(nodeName)}/log-agent`
  );
  return res.data?.data;
}
