import { useEffect } from 'react';
import { useAuth } from 'react-oidc-context';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import type { AppRole } from '@/types';
import { toast } from 'sonner';

interface ProtectedRouteProps {
  children?: React.ReactNode;
  requiredRoles?: AppRole[];
}

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
    
    // Parse the access token payload to ensure we extract Keycloak realm/resource roles reliably
    let tokenParsed: any = profile;
    if (auth.user?.access_token) {
      try {
        const payload = auth.user.access_token.split('.')[1];
        tokenParsed = { ...profile, ...JSON.parse(atob(payload)) };
      } catch (e) {
        console.error('Failed to parse access token', e);
      }
    }

    const userRoles = [
      ...(tokenParsed?.realm_access?.roles || []),
      ...(tokenParsed?.resource_access?.['eshop-client']?.roles || []), // Replace with actual client_id if different
      ...(tokenParsed?.roles || [])
    ].map((r: string) => r.toUpperCase());

    const isAllowed = requiredRoles.some(role => 
      userRoles.includes(role.toUpperCase()) || 
      userRoles.includes(`ROLE_${role.toUpperCase()}`)
    );
    
    if (!isAllowed) {
      toast.error('Bu sayfaya erişim yetkiniz bulunmamaktadır.', { id: 'unauthorized' });
      return <Navigate to="/unauthorized" replace />;
    }
  }

  if (children) return <>{children}</>;
  return <Outlet />;
}
