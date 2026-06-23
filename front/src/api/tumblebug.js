import client from './client';
import { cached } from './cache';

const TB_BASE = '/tumblebug';

export async function getNsList() {
  return cached('ns', 30000, async () => {
    const res = await client.get(`${TB_BASE}/ns`);
    return res.data?.ns || [];
  });
}

export async function getInfraList(nsId) {
  return cached(`infraList:${nsId}`, 10000, async () => {
    const res = await client.get(`${TB_BASE}/ns/${nsId}/infra`);
    return res.data?.infra || [];
  });
}

export async function getInfra(nsId, infraId) {
  return cached(`infra:${nsId}/${infraId}`, 5000, async () => {
    const res = await client.get(`${TB_BASE}/ns/${nsId}/infra/${infraId}`);
    return res.data || {};
  });
}

/** Get all Infra list with Node details for a namespace */
export async function getAllInfrasWithNodes(nsId) {
  const infras = await getInfraList(nsId);
  return infras || [];
}
