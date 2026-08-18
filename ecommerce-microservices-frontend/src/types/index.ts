// ─── Auth / Keycloak ──────────────────────────────────────────────────────────
export type AppRole = 'ROLE_ADMIN' | 'ROLE_SELLER' | 'ROLE_CUSTOMER';

export interface KeycloakTokenParsed {
  sub: string;
  email?: string;
  email_verified?: boolean;
  preferred_username?: string;
  given_name?: string;
  family_name?: string;
  name?: string;
  realm_access?: {
    roles: string[];
  };
  resource_access?: Record<string, { roles: string[] }>;
}

// ─── API Generic ─────────────────────────────────────────────────────────────
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}
