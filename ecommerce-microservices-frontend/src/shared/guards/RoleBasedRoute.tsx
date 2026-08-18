import { Navigate } from 'react-router-dom';
import { useAuth } from 'react-oidc-context';
import { hasRole } from '@/lib/auth';
import type { AppRole } from '@/types';

interface RoleBasedRouteProps {
  children: React.ReactNode;
  roles: AppRole[];
  fallback?: React.ReactNode;
}

/**
 * Renders children only if the authenticated user has one of the given roles.
 * Optionally renders a fallback element or redirects to /unauthorized.
 */
export function RoleBasedRoute({ children, roles, fallback }: RoleBasedRouteProps) {
  const auth = useAuth();

  if (!hasRole(auth, roles)) {
    if (fallback !== undefined) return <>{fallback}</>;
    return <Navigate to="/unauthorized" replace />;
  }

  return <>{children}</>;
}
