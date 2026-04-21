// Official CSP logos from mc-web-console (PNG)
const PROVIDERS = {
  azure:    { label: 'Azure',     logo: '/logos/azure.png' },
  gcp:      { label: 'GCP',       logo: '/logos/gcp.png' },
  aws:      { label: 'AWS',       logo: '/logos/aws.png' },
  openstack:{ label: 'OpenStack', logo: null },
  ncp:      { label: 'NCP',       logo: '/logos/ncp.png' },
  alibaba:  { label: 'Alibaba',   logo: '/logos/alibaba.png' },
  tencent:  { label: 'Tencent',   logo: '/logos/tencent.png' },
  ibm:      { label: 'IBM',       logo: '/logos/ibm.png' },
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
    <span className="inline-flex items-center gap-1.5 text-xs px-2 py-0.5 rounded-full bg-gray-50 border border-gray-200 font-medium text-gray-700">
      {p.logo ? (
        <img src={p.logo} alt={p.label} className="h-3.5 w-auto" />
      ) : (
        <span className="text-[10px]">{p.label.charAt(0)}</span>
      )}
      {showLabel && p.label}
    </span>
  );
}

export { detectProvider, PROVIDERS };
