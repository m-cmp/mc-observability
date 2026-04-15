import axios from 'axios';

let _token = null;

export function setApiToken(token) {
  _token = token;
}

const client = axios.create({
  baseURL: '',
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  if (_token && _token !== 'bypass') {
    config.headers.Authorization = `Bearer ${_token}`;
  }
  return config;
});

export default client;
