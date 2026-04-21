import { useLocation } from 'react-router-dom';

/** Returns '/embed' if current path starts with /embed, otherwise '' */
export default function useBasePath() {
  const { pathname } = useLocation();
  return pathname.startsWith('/embed') ? '/embed' : '';
}
