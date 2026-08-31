import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchMyOrders,
  fetchOrderById,
  createOrder,
  cancelMyOrder,
  fetchAllOrders,
  updateOrderStatus,
} from './orderService';
import type { OrderQueryParams, CreateOrderRequest, UpdateOrderStatusRequest } from './types';

// ─── Query Keys ───────────────────────────────────────────────────────────────
export const orderKeys = {
  all: ['orders'] as const,
  myOrders: () => [...orderKeys.all, 'my-orders'] as const,
  myOrderList: (params: OrderQueryParams) => [...orderKeys.myOrders(), 'list', params] as const,
  myOrderDetail: (id: string) => [...orderKeys.myOrders(), 'detail', id] as const,
  adminOrders: () => [...orderKeys.all, 'admin-orders'] as const,
  adminOrderList: (params: OrderQueryParams) => [...orderKeys.adminOrders(), 'list', params] as const,
  adminOrderDetail: (id: string) => [...orderKeys.adminOrders(), 'detail', id] as const,
};

// ─── Customer Hooks ───────────────────────────────────────────────────────────

export function useMyOrders(params: OrderQueryParams = {}) {
  return useQuery({
    queryKey: orderKeys.myOrderList(params),
    queryFn: () => fetchMyOrders(params),
    placeholderData: (prev) => prev,
  });
}

export function useMyOrder(id: string) {
  return useQuery({
    queryKey: orderKeys.myOrderDetail(id),
    queryFn: () => fetchOrderById(id),
    enabled: !!id,
  });
}

export function useCreateOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateOrderRequest) => createOrder(payload),
    onSuccess: async () => {
      await qc.invalidateQueries({ queryKey: orderKeys.myOrders() });
      // Ürün detay, ürün listesi ve vitrin sorgularını anında geçersiz kıl ve yeniden çek
      await qc.invalidateQueries({ queryKey: ['products'] });
      await qc.invalidateQueries({ queryKey: ['product'] });
      await qc.invalidateQueries({ queryKey: ['top-products'] });
      await qc.invalidateQueries({ queryKey: ['top-50-products'] });
      await qc.refetchQueries({ queryKey: ['product'] });
    },
  });
}

export function useCancelMyOrder() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => cancelMyOrder(id),
    onSuccess: (data) => {
      qc.setQueryData(orderKeys.myOrderDetail(data.id), data);
      qc.invalidateQueries({ queryKey: orderKeys.myOrders() });
      // Invalidate both catalog list and specific product details
      qc.invalidateQueries({ queryKey: ['products'] });
      data.items?.forEach(item => {
        qc.invalidateQueries({ queryKey: ['products', 'detail', item.productId] });
      });
    },
  });
}

// ─── Admin Hooks ──────────────────────────────────────────────────────────────

export function useAdminOrders(params: OrderQueryParams = {}) {
  return useQuery({
    queryKey: orderKeys.adminOrderList(params),
    queryFn: () => fetchAllOrders(params),
    placeholderData: (prev) => prev,
  });
}

export function useAdminOrder(id: string) {
  return useQuery({
    queryKey: orderKeys.adminOrderDetail(id),
    queryFn: () => fetchOrderById(id),
    enabled: !!id,
  });
}

export function useUpdateOrderStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateOrderStatusRequest }) =>
      updateOrderStatus(id, payload),
    onSuccess: (data) => {
      qc.setQueryData(orderKeys.adminOrderDetail(data.id), data);
      qc.invalidateQueries({ queryKey: orderKeys.adminOrders() });
      // Invalidate both catalog list and specific product details
      qc.invalidateQueries({ queryKey: ['products'] });
      data.items?.forEach(item => {
        qc.invalidateQueries({ queryKey: ['products', 'detail', item.productId] });
      });
    },
  });
}
