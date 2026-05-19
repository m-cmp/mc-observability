import client from './client';

// Trigger policies
export async function getPolicies(page = 1, size = 20) {
  const res = await client.get('/api/o11y/trigger/policy', { params: { page, size } });
  return res.data?.data || {};
}

export async function createPolicy(body) {
  const res = await client.post('/api/o11y/trigger/policy', body);
  return res.data?.data || res.data;
}

export async function deletePolicy(id) {
  return client.delete(`/api/o11y/trigger/policy/${id}`);
}

export async function addVmToPolicy(policyId, body) {
  return client.post(`/api/o11y/trigger/policy/${policyId}/vm`, body);
}

export async function removeVmFromPolicy(policyId, body) {
  return client.delete(`/api/o11y/trigger/policy/${policyId}/vm`, { data: body });
}

export async function updatePolicyChannels(policyId, channels) {
  return client.put(`/api/o11y/trigger/policy/${policyId}/channel`, channels);
}

// Alerts
export async function getAlerts() {
  const res = await client.get('/api/o11y/trigger/alert/alerts');
  return res.data || [];
}

export async function getAlertRules() {
  const res = await client.get('/api/o11y/trigger/alert/alert-rules');
  return res.data || [];
}

// Notification
export async function getNotiChannels() {
  const res = await client.get('/api/o11y/trigger/noti/channel');
  return res.data?.data || {};
}

export async function getNotiHistory(page = 1, size = 20) {
  const res = await client.get('/api/o11y/trigger/noti/history', { params: { page, size } });
  return res.data?.data || {};
}

// Trigger history
export async function getTriggerHistory(page = 1, size = 20) {
  const res = await client.get('/api/o11y/trigger/history', { params: { page, size } });
  return res.data?.data || {};
}
