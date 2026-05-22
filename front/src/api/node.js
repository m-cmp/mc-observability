import client from './client';

// NOTE: mc-observability backend API path still uses `/mci/{}/vm/{}` literals
// even though Tumblebug renamed MCI→Infra / VM→Node. URL segments stay as-is;
// only JS identifiers reflect the new naming.

export async function getNodeList(nsId, infraId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${infraId}/vm`);
  return res.data?.data || [];
}

export async function getNode(nsId, infraId, nodeId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${infraId}/vm/${nodeId}`);
  return res.data?.data || res.data?.responseData || null;
}

export async function installAgent(nsId, infraId, nodeId) {
  const res = await client.post(`/api/o11y/monitoring/${nsId}/${infraId}/vm/${nodeId}`, { name: nodeId });
  return res.data;
}

export async function uninstallAgent(nsId, infraId, nodeId) {
  const res = await client.delete(`/api/o11y/monitoring/${nsId}/${infraId}/vm/${nodeId}`);
  return res.data;
}

export async function getNodeItems(nsId, infraId, nodeId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${infraId}/vm/${nodeId}/item`);
  return res.data?.data || [];
}

export async function createNodeItem(nsId, infraId, nodeId, item) {
  const body = { pluginSeq: item.pluginSeq, pluginConfig: item.pluginConfig || '' };
  const res = await client.post(`/api/o11y/monitoring/${nsId}/${infraId}/vm/${nodeId}/item`, body);
  return res.data;
}

export async function updateNodeItem(nsId, infraId, nodeId, item) {
  const res = await client.put(`/api/o11y/monitoring/${nsId}/${infraId}/vm/${nodeId}/item`, item);
  return res.data;
}

export async function deleteNodeItem(nsId, infraId, nodeId, itemSeq) {
  const res = await client.delete(`/api/o11y/monitoring/${nsId}/${infraId}/vm/${nodeId}/item/${itemSeq}`);
  return res.data;
}
