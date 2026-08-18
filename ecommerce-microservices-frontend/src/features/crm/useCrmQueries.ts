import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchMyProfile,
  createProfile,
  updateProfile,
  fetchAddresses,
  createAddress,
  updateAddress,
  deleteAddress,
  fetchAllUsers,
  assignRole,
  removeRole,
} from './crmService';
import type { CreateProfileRequest, UpdateProfileRequest, AddressRequest } from './types';
import type { AppRole } from '@/types';

// ─── Query Keys ───────────────────────────────────────────────────────────────
export const crmKeys = {
  profile: ['crm', 'profile'] as const,
  addresses: ['crm', 'addresses'] as const,
  users: ['crm', 'admin', 'users'] as const,
  user: (id: string) => ['crm', 'admin', 'users', id] as const,
};

// ─── Profile Hooks ────────────────────────────────────────────────────────────

export function useProfile() {
  return useQuery({
    queryKey: crmKeys.profile,
    queryFn: fetchMyProfile,
    retry: (failureCount, error: unknown) => {
      // Don't retry on 404 — profile may not exist yet (first login)
      const e = error as { response?: { status?: number } };
      if (e?.response?.status === 404) return false;
      return failureCount < 2;
    },
  });
}

export function useCreateProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateProfileRequest) => createProfile(payload),
    onSuccess: (data) => {
      qc.setQueryData(crmKeys.profile, data);
    },
  });
}

export function useUpdateProfile() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateProfileRequest) => updateProfile(payload),
    onSuccess: (data) => {
      qc.setQueryData(crmKeys.profile, data);
    },
  });
}

// ─── Address Hooks ────────────────────────────────────────────────────────────

export function useAddresses() {
  return useQuery({
    queryKey: crmKeys.addresses,
    queryFn: fetchAddresses,
  });
}

export function useCreateAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: AddressRequest) => createAddress(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: crmKeys.addresses });
    },
  });
}

export function useUpdateAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: AddressRequest }) =>
      updateAddress(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: crmKeys.addresses });
    },
  });
}

export function useDeleteAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteAddress(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: crmKeys.addresses });
    },
  });
}


// ─── Admin Hooks ──────────────────────────────────────────────────────────────

export function useAdminUsers() {
  return useQuery({
    queryKey: crmKeys.users,
    queryFn: fetchAllUsers,
  });
}

export function useAssignRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: AppRole }) =>
      assignRole(userId, role),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: crmKeys.users });
    },
  });
}

export function useRemoveRole() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ userId, role }: { userId: string; role: AppRole }) =>
      removeRole(userId, role),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: crmKeys.users });
    },
  });
}
