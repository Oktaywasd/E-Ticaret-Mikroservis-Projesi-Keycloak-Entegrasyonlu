import type { AuthContextProps } from 'react-oidc-context';
import type { AppRole, KeycloakTokenParsed } from '@/types';

/**
 * Extract realm roles from the parsed OIDC token profile.
 */
export function getRoles(auth: AuthContextProps): AppRole[] {
  const profile = auth.user?.profile as any;
  let tokenParsed: any = profile;
  if (auth.user?.access_token) {
    try {
      const payload = auth.user.access_token.split('.')[1];
      tokenParsed = { ...profile, ...JSON.parse(atob(payload)) };
    } catch (e) {
      console.error('Failed to parse access token', e);
    }
  }

  const roles = [
    ...(tokenParsed?.realm_access?.roles || []),
    ...(tokenParsed?.resource_access?.['eshop-client']?.roles || []),
    ...(tokenParsed?.roles || [])
  ].map((r: string) => r.toUpperCase());
  
  return roles as AppRole[];
}

/**
 * Returns true if the user has at least one of the given roles.
 */
export function hasRole(auth: AuthContextProps, roles: AppRole[]): boolean {
  const userRoles = getRoles(auth);
  return roles.some((role) => 
    userRoles.includes(role.toUpperCase() as AppRole) || 
    userRoles.includes(`ROLE_${role.toUpperCase()}` as AppRole)
  );
}

/**
 * Returns the access token string from the authenticated user.
 */
export function getAccessToken(auth: AuthContextProps): string | undefined {
  return auth.user?.access_token;
}
