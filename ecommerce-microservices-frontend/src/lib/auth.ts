import type { AuthContextProps } from 'react-oidc-context';
import type { AppRole, KeycloakTokenParsed } from '@/types';

/**
 * Extract realm roles from the parsed OIDC token profile.
 */
export function getRoles(auth: AuthContextProps): AppRole[] {
  const profile = auth.user?.profile as any;
  const roles = [
    ...(profile?.realm_access?.roles || []),
    ...(profile?.resource_access?.['eshop-client']?.roles || []),
    ...(profile?.roles || [])
  ];
  return roles as AppRole[];
}

/**
 * Returns true if the user has at least one of the given roles.
 */
export function hasRole(auth: AuthContextProps, roles: AppRole[]): boolean {
  const userRoles = getRoles(auth);
  return roles.some((r) => userRoles.includes(r));
}

/**
 * Returns the access token string from the authenticated user.
 */
export function getAccessToken(auth: AuthContextProps): string | undefined {
  return auth.user?.access_token;
}
