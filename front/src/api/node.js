import client from './client';

// mc-observability backend API: /api/o11y/monitoring/{nsId}/{infraId}/node/{nodeId}
// (MCI→Infra, VM→Node naming applied across path segments and identifiers).

export async function getNodeList(nsId, infraId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${infraId}/node`);
  return res.data?.data || [];
}

export async function getNode(nsId, infraId, nodeId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}`);
  return res.data?.data || res.data?.responseData || null;
}

export async function installAgent(nsId, infraId, nodeId) {
  const res = await client.post(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}`, { name: nodeId });
  return res.data;
}

export async function uninstallAgent(nsId, infraId, nodeId) {
  const res = await client.delete(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}`);
  return res.data;
}

// Per-agent install/uninstall (monitoring = telegraf, log = fluent-bit) — managed independently.
export async function installMonitoringAgent(nsId, infraId, nodeId) {
  return (await client.post(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/monitoring-agent`)).data;
}
export async function uninstallMonitoringAgent(nsId, infraId, nodeId) {
  return (await client.delete(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/monitoring-agent`)).data;
}
export async function installLogAgent(nsId, infraId, nodeId) {
  return (await client.post(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/log-agent`)).data;
}
export async function uninstallLogAgent(nsId, infraId, nodeId) {
  return (await client.delete(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/log-agent`)).data;
}

export async function getNodeItems(nsId, infraId, nodeId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/item`);
  return res.data?.data || [];
}

export async function createNodeItem(nsId, infraId, nodeId, item) {
  const body = { pluginSeq: item.pluginSeq, pluginConfig: item.pluginConfig || '' };
  const res = await client.post(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/item`, body);
  return res.data;
}

export async function updateNodeItem(nsId, infraId, nodeId, item) {
  const res = await client.put(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/item`, item);
  return res.data;
}

export async function deleteNodeItem(nsId, infraId, nodeId, itemSeq) {
  const res = await client.delete(`/api/o11y/monitoring/${nsId}/${infraId}/node/${nodeId}/item/${itemSeq}`);
  return res.data;
}
