import { crmApi } from '@/lib/axios';
import type {
  UserProfile,
  CreateProfileRequest,
  UpdateProfileRequest,
  Address,
  AddressRequest,
  AdminUser,
} from './types';
import type { AppRole } from '@/types';

// ─── Profile & Auth ───────────────────────────────────────────────────────────

export async function register(payload: any): Promise<any> {
  const { data } = await crmApi.post('/auth/register', payload);
  return data;
}

/** Get the authenticated user's profile */
export async function fetchMyProfile(): Promise<UserProfile> {
  const { data } = await crmApi.get<UserProfile>('/profile/me');
  return data;
}

/** Create profile on first login / onboarding */
export async function createProfile(payload: CreateProfileRequest): Promise<UserProfile> {
  const { data } = await crmApi.post<UserProfile>('/profile', payload);
  return data;
}

/** Update authenticated user's profile */
export async function updateProfile(payload: UpdateProfileRequest): Promise<UserProfile> {
  const { data } = await crmApi.patch<UserProfile>('/profile/me', payload);
  return data;
}

// ─── Addresses ────────────────────────────────────────────────────────────────

export async function fetchAddresses(): Promise<Address[]> {
  const { data } = await crmApi.get<Address[]>('/addresses');
  return data;
}

export async function fetchAddressById(id: string): Promise<Address> {
  const { data } = await crmApi.get<Address>(`/addresses/${id}`);
  return data;
}

export async function createAddress(addressData: any): Promise<Address> {
  const payload = {
    title: addressData.title || addressData.addressTitle,
    street: addressData.street || addressData.addressLine,
    city: addressData.city,
    state: addressData.state || addressData.district,
    zipCode: addressData.zipCode,
    country: addressData.country
  };

  const { data } = await crmApi.post<Address>('/addresses', payload);
  return data;
}

export async function updateAddress(id: string, payload: AddressRequest): Promise<Address> {
  const { data } = await crmApi.patch<Address>(`/addresses/${id}`, payload);
  return data;
}

export async function deleteAddress(id: string): Promise<void> {
  await crmApi.delete(`/addresses/${id}`);
}



// ─── Admin — Users ────────────────────────────────────────────────────────────
// Admin endpoint'leri belirtilmemiş, eski haliyle "/admin/users" kalsın.

export async function fetchAllUsers(): Promise<AdminUser[]> {
  const { data } = await crmApi.get<AdminUser[]>('/admin/users');
  return data;
}

export async function fetchUserById(userId: string): Promise<AdminUser> {
  const { data } = await crmApi.get<AdminUser>(`/admin/users/${userId}`);
  return data;
}

export async function assignRole(userId: string, role: AppRole): Promise<void> {
  await crmApi.post(`/admin/users/${userId}/roles`, { role });
}

export async function removeRole(userId: string, role: AppRole): Promise<void> {
  await crmApi.delete(`/admin/users/${userId}/roles/${role}`);
}
