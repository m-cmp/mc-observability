import client from './client';

const TB_BASE = '/tumblebug';

export async function getNsList() {
  const res = await client.get(`${TB_BASE}/ns`);
  return res.data?.ns || [];
}

export async function getMciList(nsId) {
  const res = await client.get(`${TB_BASE}/ns/${nsId}/mci`);
  return res.data?.mci || [];
}

export async function getMci(nsId, mciId) {
  const res = await client.get(`${TB_BASE}/ns/${nsId}/mci/${mciId}`);
  return res.data || {};
}
