import client from './client';

const TB_BASE = '/tumblebug';

export async function getNsList() {
  const res = await client.get(`${TB_BASE}/ns`);
  return res.data?.ns || [];
}

export async function getInfraList(nsId) {
  const res = await client.get(`${TB_BASE}/ns/${nsId}/infra`);
  return res.data?.infra || [];
}

export async function getInfra(nsId, infraId) {
  const res = await client.get(`${TB_BASE}/ns/${nsId}/infra/${infraId}`);
  return res.data || {};
}

/** Get all Infra list with Node details for a namespace */
export async function getAllInfrasWithNodes(nsId) {
  const infras = await getInfraList(nsId);
  return infras || [];
}
