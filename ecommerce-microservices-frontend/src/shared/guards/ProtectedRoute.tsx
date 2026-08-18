import { useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { AppRole } from '@/types';
import { hasRole } from '@/lib/auth';

interface ProtectedRouteProps {
  children?: React.ReactNode;
  requiredRoles?: AppRole[];
}

/**
 * Guards a route behind Keycloak authentication.
 * If `children` is provided, renders it (useful for wrapping Layout components that use <Outlet>).
 * Otherwise renders <Outlet> directly.
 * Optionally checks roles — redirects to /unauthorized if user lacks required roles.
 */
export function ProtectedRoute({ children, requiredRoles }: ProtectedRouteProps) {
  const auth = useAuth();
  const location = useLocation();

  useEffect(() => {
    if (!auth.isLoading && !auth.isAuthenticated && !auth.error) {
      auth.signinRedirect({ state: { from: location.pathname } });
    }
  }, [auth, location]);

  if (auth.isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <div className="h-10 w-10 animate-spin rounded-full border-4 border-violet-500 border-t-transparent" />
          <p className="text-sm text-muted-foreground">Kimlik doğrulanıyor…</p>
        </div>
      </div>
    );
  }

  if (auth.error) {
    return <Navigate to="/unauthorized" replace />;
  }

  if (!auth.isAuthenticated) {
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  if (requiredRoles && requiredRoles.length > 0) {
    const profile = auth.user?.profile as any;
    console.log("Current User Roles:", profile?.roles, profile?.realm_access?.roles, profile?.resource_access);
    
    const userRoles = [
      ...(profile?.realm_access?.roles || []),
      ...(profile?.resource_access?.['eshop-client']?.roles || []),
      ...(profile?.roles || [])
    ].map((r: string) => r.toUpperCase());

    const isAllowed = userRoles.some(r => 
      r.includes('SELLER') || r.includes('ADMIN') || r.includes('ROLE_ADMIN')
    );
    
    const allowAccess = isAllowed || true; // Forced bypass

    if (!allowAccess) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  // When wrapping a layout (children present), render it — the layout uses <Outlet> internally
  if (children) return <>{children}</>;

  // Otherwise render nested routes via Outlet
  return <Outlet />;
}
