import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  fetchProducts,
  fetchProductById,
  fetchCategories,
  createProduct,
  updateProduct,
  updateProductStock,
  deleteProduct,
  toggleProductStatus,
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

export function useToggleProductStatus() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => toggleProductStatus(id),
    onSuccess: (updated) => {
      qc.setQueryData(productKeys.detail(updated.id), updated);
      
      // Update lists in cache instantly without causing a refetch that might hide it
      qc.setQueriesData(
        { queryKey: productKeys.lists() },
        (oldData: any) => {
          if (!oldData || !oldData.content) return oldData;
          return {
            ...oldData,
            content: oldData.content.map((p: any) => 
              p.id === updated.id ? { ...p, isActive: updated.isActive ?? updated.active, active: updated.active ?? updated.isActive } : p
            )
          };
        }
      );
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
