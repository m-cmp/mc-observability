import client from './client';

export async function getVmList(nsId, mciId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${mciId}/vm`);
  return res.data?.data || [];
}

export async function getVm(nsId, mciId, vmId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${mciId}/vm/${vmId}`);
  return res.data?.data || res.data?.responseData || null;
}

export async function installAgent(nsId, mciId, vmId) {
  const res = await client.post(`/api/o11y/monitoring/${nsId}/${mciId}/vm/${vmId}`, { name: vmId });
  return res.data;
}

export async function uninstallAgent(nsId, mciId, vmId) {
  const res = await client.delete(`/api/o11y/monitoring/${nsId}/${mciId}/vm/${vmId}`);
  return res.data;
}

export async function getVmItems(nsId, mciId, vmId) {
  const res = await client.get(`/api/o11y/monitoring/${nsId}/${mciId}/vm/${vmId}/item`);
  return res.data?.data || [];
}

export async function createVmItem(nsId, mciId, vmId, item) {
  const body = { pluginSeq: item.pluginSeq, pluginConfig: item.pluginConfig || '' };
  const res = await client.post(`/api/o11y/monitoring/${nsId}/${mciId}/vm/${vmId}/item`, body);
  return res.data;
}

export async function updateVmItem(nsId, mciId, vmId, item) {
  const res = await client.put(`/api/o11y/monitoring/${nsId}/${mciId}/vm/${vmId}/item`, item);
  return res.data;
}

export async function deleteVmItem(nsId, mciId, vmId, itemSeq) {
  const res = await client.delete(`/api/o11y/monitoring/${nsId}/${mciId}/vm/${vmId}/item/${itemSeq}`);
  return res.data;
}
