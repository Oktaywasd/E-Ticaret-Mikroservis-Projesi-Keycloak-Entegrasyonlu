import type { AppRole } from '@/types';

// ─── User Profile ─────────────────────────────────────────────────────────────
export interface UserProfile {
  id: string;
  keycloakId: string;       // JWT sub
  email: string;
  firstName: string;
  lastName: string;
  phoneNumber?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateProfileRequest {
  firstName: string;
  lastName: string;
  phoneNumber?: string;
}

export interface UpdateProfileRequest extends Partial<CreateProfileRequest> {}

// ─── Address ──────────────────────────────────────────────────────────────────
export interface Address {
  id: string;
  addressTitle?: string;
  addressLine?: string;
  district?: string;
  title?: string;
  street?: string;
  state?: string;
  city: string;
  zipCode: string;
  country: string;
}

export interface AddressRequest {
  title: string;
  street: string;
  state: string;
  city: string;
  zipCode: string;
  country: string;
}

// ─── Admin — Users ────────────────────────────────────────────────────────────
export interface AdminUser {
  id: string;               // Keycloak user ID
  username: string;
  email: string;
  firstName?: string;
  lastName?: string;
  enabled: boolean;
  roles: AppRole[];
  createdTimestamp?: number;
}

export interface RoleAssignRequest {
  role: AppRole;
}
