const PROVIDERS = {
  azure: {
    label: 'Azure',
    color: '#0078D4',
    icon: (
      <svg viewBox="0 0 18 18" fill="currentColor" className="w-3.5 h-3.5">
        <path d="M6.5 0L1 9.2l3.3 5.5h2.3L2.9 9.2 6.5 2h4.1L7 9.2l3.6 5.5H14l4-7.5L14.3 0H6.5z" />
      </svg>
    ),
  },
  gcp: {
    label: 'GCP',
    color: '#4285F4',
    icon: (
      <svg viewBox="0 0 18 18" fill="none" className="w-3.5 h-3.5">
        <path d="M11.2 3.6L12.6 1.2C12.8 0.9 12.7 0.5 12.4 0.3 11.3 -0.3 10.2 -0.1 9 0.1 7.8 0.3 6.7 0.9 5.8 1.7L7.5 3.8C8.6 2.9 10 2.9 11.2 3.6Z" fill="#EA4335"/>
        <path d="M14.4 5.4C13.6 4.1 12.5 3.3 11.2 3.6L7.5 3.8 5.8 1.7C4.5 3 3.6 4.8 3.6 6.8H7.2C7.2 5.9 7.8 5.1 8.7 4.8L14.4 5.4Z" fill="#4285F4"/>
        <path d="M3.6 6.8C3.6 8.8 4.2 10.6 5.4 12L7.5 9.6C6.6 8.9 6 7.9 6 6.8H3.6Z" fill="#FBBC05"/>
        <path d="M9 18C11.2 18 13.1 17.2 14.4 15.8L12 13.8C11.1 14.5 10.1 15 9 15 7.2 15 5.7 13.8 5.1 12.2L3 14.4C4.5 16.6 6.6 18 9 18Z" fill="#34A853"/>
        <path d="M18 9C18 8.4 17.9 7.7 17.8 7.2H9V10.8H14C13.7 12 13 13 12 13.8L14.4 15.8C16.6 13.8 18 11.6 18 9Z" fill="#4285F4"/>
      </svg>
    ),
  },
  aws: {
    label: 'AWS',
    color: '#FF9900',
    icon: (
      <svg viewBox="0 0 18 18" fill="currentColor" className="w-3.5 h-3.5">
        <path d="M5.1 7.7c0 .3 0 .5.1.7.1.2.2.4.3.5.1.1.1.2.1.2s-.1.1-.2.2l-.7.5c-.1 0-.1.1-.2.1-.1 0-.1 0-.2-.1-.1-.1-.2-.3-.3-.4-.1-.2-.2-.3-.3-.5C3 9.5 2.2 10 1.2 10c-.7 0-1.3-.2-1.7-.6C.2 9 0 8.5 0 7.8c0-.7.3-1.3.8-1.7.5-.4 1.2-.6 2.1-.6.3 0 .6 0 .9.1.3.1.6.1 1 .2v-.6c0-.6-.1-1.1-.4-1.3C4.1 3.6 3.6 3.5 3 3.5c-.3 0-.7 0-1 .1-.4.1-.7.2-1 .3-.2.1-.3.1-.3.1H.5c-.1 0-.2-.1-.2-.3v-.5c0-.1 0-.2.1-.3.1 0 .1-.1.3-.2.3-.2.7-.3 1.2-.4C2.3 2.2 2.8 2.1 3.4 2.1c1 0 1.7.2 2.2.7.4.5.7 1.1.7 2v2.9h-.2zM1.7 8.7c.3 0 .5 0 .8-.1.3-.1.5-.3.7-.5.1-.1.2-.3.3-.5V6.8c-.2-.1-.5-.1-.8-.2-.3 0-.5-.1-.8-.1-.6 0-1 .1-1.3.3-.3.2-.4.5-.4.9 0 .4.1.6.3.8.2.2.6.2 1.2.2zM10 9.8c-.1 0-.2 0-.3-.1-.1 0-.1-.1-.2-.3L7.3 2.6c0-.1-.1-.2-.1-.3 0-.1.1-.2.2-.2h1.1c.1 0 .3 0 .3.1.1 0 .1.1.2.3L10.7 8l1.6-5.5c0-.1.1-.2.2-.3.1 0 .2-.1.3-.1h.9c.1 0 .3 0 .3.1.1.1.1.2.2.3l1.6 5.6 1.8-5.6c0-.1.1-.2.2-.3.1 0 .2-.1.3-.1h1c.1 0 .2.1.2.2 0 0 0 .1 0 .1s0 .1-.1.2l-2.3 6.8c0 .1-.1.2-.2.3-.1 0-.2.1-.3.1h-1c-.1 0-.3 0-.3-.1-.1-.1-.1-.2-.2-.3l-1.6-5.4-1.6 5.4c0 .1-.1.2-.2.3-.1.1-.2.1-.3.1H10z" />
        <path d="M17.5 13.5c-2.3 1.7-5.5 2.5-8.4 2.5-4 0-7.5-1.5-10.2-3.9-.2-.2 0-.5.2-.3 2.9 1.7 6.5 2.7 10.2 2.7 2.5 0 5.3-.5 7.8-1.6.4-.2.7.2.4.6z" />
      </svg>
    ),
  },
  openstack: {
    label: 'OpenStack',
    color: '#ED1944',
    icon: (
      <svg viewBox="0 0 18 18" fill="currentColor" className="w-3.5 h-3.5">
        <rect x="1" y="1" width="7" height="7" rx="1" />
        <rect x="10" y="1" width="7" height="7" rx="1" />
        <rect x="1" y="10" width="7" height="7" rx="1" />
        <rect x="10" y="10" width="7" height="7" rx="1" opacity="0.5" />
      </svg>
    ),
  },
  ncp: {
    label: 'NCP',
    color: '#03CF5D',
    icon: (
      <svg viewBox="0 0 18 18" fill="currentColor" className="w-3.5 h-3.5">
        <path d="M2 4h4l5 10H7L2 4zM11 4h5v2h-3.5l2.5 5h-3L9 4h2z" />
      </svg>
    ),
  },
  alibaba: {
    label: 'Alibaba',
    color: '#FF6A00',
    icon: (
      <svg viewBox="0 0 18 18" fill="currentColor" className="w-3.5 h-3.5">
        <path d="M9 2C5.1 2 2 5.1 2 9s3.1 7 7 7 7-3.1 7-7-3.1-7-7-7zm0 12c-2.8 0-5-2.2-5-5s2.2-5 5-5 5 2.2 5 5-2.2 5-5 5z" />
        <circle cx="9" cy="9" r="2" />
      </svg>
    ),
  },
  tencent: {
    label: 'Tencent',
    color: '#00A4FF',
    icon: (
      <svg viewBox="0 0 18 18" fill="currentColor" className="w-3.5 h-3.5">
        <path d="M9 1C4.6 1 1 4.6 1 9s3.6 8 8 8 8-3.6 8-8-3.6-8-8-8zm0 14c-3.3 0-6-2.7-6-6s2.7-6 6-6 6 2.7 6 6-2.7 6-6 6z" />
        <path d="M7 7h4v4H7z" />
      </svg>
    ),
  },
  ibm: {
    label: 'IBM',
    color: '#054ADA',
    icon: (
      <svg viewBox="0 0 18 18" fill="currentColor" className="w-3.5 h-3.5">
        <path d="M1 3h6v2H1zM1 7h6v2H1zM1 11h6v2H1zM9 3h3v2H9zM9 7h3l2 2H9zM9 11h3v2H9zM14 3h3v2h-3zM14 11h3v2h-3z" />
      </svg>
    ),
  },
};

function detectProvider(connectionName) {
  if (!connectionName) return null;
  const lower = connectionName.toLowerCase();
  for (const key of Object.keys(PROVIDERS)) {
    if (lower.includes(key)) return key;
  }
  return null;
}

export default function ProviderBadge({ connectionName, showLabel = true }) {
  const key = detectProvider(connectionName);
  if (!key) {
    const fallback = (connectionName || '').split('-')[0].toUpperCase();
    return <span className="text-xs px-2 py-0.5 rounded-full bg-gray-100 text-gray-500 font-medium">{fallback}</span>;
  }
  const p = PROVIDERS[key];
  return (
    <span className="inline-flex items-center gap-1 text-xs px-2 py-0.5 rounded-full font-medium"
      style={{ backgroundColor: p.color + '15', color: p.color }}>
      {p.icon}
      {showLabel && p.label}
    </span>
  );
}

export { detectProvider, PROVIDERS };
