import { useAuth } from 'react-oidc-context';
import { getRoles, hasRole } from '@/lib/auth';
import type { AppRole } from '@/types';

/**
 * Convenience hook wrapping react-oidc-context with role utilities.
 */
export function useAppAuth() {
  const auth = useAuth();

  const roles = getRoles(auth);

  return {
    ...auth,
    roles,
    hasRole: (...r: AppRole[]) => hasRole(auth, r),
    isAdmin: hasRole(auth, ['ROLE_ADMIN']),
    isSeller: hasRole(auth, ['ROLE_SELLER']),
    isCustomer: hasRole(auth, ['ROLE_CUSTOMER']),
    isAdminOrSeller: roles.some((role: any) => ['ROLE_ADMIN', 'ADMIN', 'SELLER'].includes(role)),
    displayName:
      auth.user?.profile?.name ??
      auth.user?.profile?.preferred_username ??
      'Kullanıcı',
    email: auth.user?.profile?.email,
  };
}
