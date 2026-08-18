import { orderApi } from '@/lib/axios';
import type { PagedResponse } from '@/types';
import type { Order, CreateOrderRequest, UpdateOrderStatusRequest, OrderQueryParams } from './types';

// ─── Customer & General Orders ───────────────────────────────────────────────

export async function fetchMyOrders(params: OrderQueryParams = {}): Promise<PagedResponse<Order>> {
  const { data } = await orderApi.get<PagedResponse<Order>>('/orders/my-orders', { params });
  return data;
}

export async function fetchOrderById(id: string): Promise<Order> {
  const { data } = await orderApi.get<Order>(`/orders/${id}`);
  return data;
}

export async function createOrder(payload: CreateOrderRequest): Promise<Order> {
  const { data } = await orderApi.post<Order>('/orders', payload);
  return data;
}

export async function cancelMyOrder(id: string): Promise<Order> {
  const { data } = await orderApi.put<Order>(`/orders/${id}/cancel`);
  return data;
}

// ─── Admin Orders ─────────────────────────────────────────────────────────────

export async function fetchAllOrders(params: OrderQueryParams = {}): Promise<PagedResponse<Order>> {
  const { data } = await orderApi.get<PagedResponse<Order>>('/orders', { params });
  return data;
}

export async function updateOrderStatus(id: string, payload: UpdateOrderStatusRequest): Promise<Order> {
  const { data } = await orderApi.put<Order>(`/orders/${id}/status`, payload);
  return data;
}
