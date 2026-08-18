import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchProducts,
  fetchProductsByCategory,
  fetchProductById,
  fetchCategories,
  createProduct,
  updateProduct,
  updateProductStock,
  deleteProduct,
  createCategory,
  updateCategory,
  deleteCategory,
} from './productService';
import type { ProductQueryParams, CreateProductRequest, UpdateProductRequest, StockUpdateRequest, CreateCategoryRequest } from './types';

// ─── Query Keys ───────────────────────────────────────────────────────────────
export const productKeys = {
  all: ['products'] as const,
  lists: () => [...productKeys.all, 'list'] as const,
  list: (params: ProductQueryParams) => [...productKeys.lists(), params] as const,
  details: () => [...productKeys.all, 'detail'] as const,
  detail: (id: string) => [...productKeys.details(), id] as const,
  categories: ['categories'] as const,
};

// ─── Query Hooks ──────────────────────────────────────────────────────────────

export function useProducts(params: ProductQueryParams = {}) {
  return useQuery({
    queryKey: productKeys.list(params),
    queryFn: () => fetchProducts(params),
    placeholderData: (prev) => prev, // keep previous data while fetching (pagination)
  });
}

export function useProductsByCategory(categoryId: string, params: ProductQueryParams = {}) {
  return useQuery({
    queryKey: ['products', 'category', categoryId, params],
    queryFn: () => fetchProductsByCategory(categoryId, params),
    placeholderData: (prev) => prev,
    enabled: !!categoryId,
  });
}

export function useProduct(id: string) {
  return useQuery({
    queryKey: productKeys.detail(id),
    queryFn: () => fetchProductById(id),
    enabled: !!id,
  });
}

export function useCategories() {
  return useQuery({
    queryKey: productKeys.categories,
    queryFn: fetchCategories,
    staleTime: 1000 * 60 * 10, // categories change rarely
  });
}

// ─── Mutation Hooks ───────────────────────────────────────────────────────────

export function useCreateProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateProductRequest) => createProduct(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

export function useUpdateProduct(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: UpdateProductRequest) => updateProduct(id, payload),
    onSuccess: (updated) => {
      qc.setQueryData(productKeys.detail(id), updated);
      qc.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

export function useUpdateProductStock(id: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: StockUpdateRequest) => updateProductStock(id, payload),
    onSuccess: (updated) => {
      qc.setQueryData(productKeys.detail(id), updated);
      qc.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

export function useDeleteProduct() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteProduct(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
}

export function useCreateCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateCategoryRequest) => createCategory(payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: productKeys.categories });
    },
  });
}

export function useUpdateCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: CreateCategoryRequest }) =>
      updateCategory(id, payload),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: productKeys.categories });
    },
  });
}

export function useDeleteCategory() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => deleteCategory(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: productKeys.categories });
    },
  });
}
