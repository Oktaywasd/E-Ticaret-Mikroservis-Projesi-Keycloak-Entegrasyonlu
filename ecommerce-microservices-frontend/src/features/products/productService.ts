import { productApi } from '@/lib/axios';
import type { PagedResponse } from '@/types';
import type {
  Product,
  Category,
  CreateProductRequest,
  UpdateProductRequest,
  StockUpdateRequest,
  CreateCategoryRequest,
  ProductQueryParams,
  ProductSuggestion,
} from './types';

// Helper to format params for API
function buildApiParams(params: ProductQueryParams) {
  const { sort, ...rest } = params;
  let sortBy, sortDirection;
  
  if (sort) {
    const parts = sort.split(',');
    if (parts.length === 2) {
      sortBy = parts[0];
      sortDirection = parts[1];
    }
  }

  // Remove undefined, null, or empty string values
  const cleanedParams: Record<string, any> = {};
  Object.entries(rest).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      cleanedParams[key] = value;
    }
  });

  if (sortBy) {
    cleanedParams.sortBy = sortBy;
    cleanedParams.sortDirection = sortDirection;
  }

  return cleanedParams;
}

export async function fetchProducts(params: ProductQueryParams = {}): Promise<PagedResponse<Product>> {
  const apiParams = buildApiParams(params);
  const { data } = await productApi.get<PagedResponse<Product>>('/products', { params: apiParams });
  return data;
}



export async function getSuggestions(query: string, limit = 5): Promise<ProductSuggestion[]> {
  const { data } = await productApi.get<ProductSuggestion[]>(`/products/suggestions`, {
    params: { q: query, limit }
  });
  return data;
}

export async function fetchProductById(id: string): Promise<Product> {
  const { data } = await productApi.get<Product>(`/products/${id}`);
  return data;
}

export async function fetchProductByCode(code: string): Promise<Product> {
  const { data } = await productApi.get<Product>(`/products/code/${code}`);
  return data;
}

export async function createProduct(payload: CreateProductRequest): Promise<Product> {
  const { data } = await productApi.post<Product>('/products', payload);
  return data;
}

export async function updateProduct(id: string, payload: UpdateProductRequest): Promise<Product> {
  const { data } = await productApi.patch<Product>(`/products/${id}`, payload);
  return data;
}

export async function uploadProductImages(id: string, files: File[]): Promise<Product> {
  const formData = new FormData();
  files.forEach(file => {
    formData.append('files', file);
  });
  const { data } = await productApi.post<Product>(`/products/${id}/images`, formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
  return data;
}

export async function updateProductStock(id: string, payload: StockUpdateRequest): Promise<Product> {
  const { data } = await productApi.put<Product>(`/products/${id}/reduce-stock`, payload);
  return data;
}

export async function toggleProductStatus(id: string): Promise<Product> {
  const { data } = await productApi.patch<Product>(`/products/${id}/toggle-status`);
  return data;
}

export async function deleteProduct(id: string): Promise<void> {
  await productApi.delete(`/products/${id}`);
}

// ─── Categories ───────────────────────────────────────────────────────────────

export async function fetchCategories(): Promise<Category[]> {
  const { data } = await productApi.get<Category[]>('/categories');
  return data;
}

export async function fetchCategoryById(id: string): Promise<Category> {
  const { data } = await productApi.get<Category>(`/categories/${id}`);
  return data;
}

export async function createCategory(payload: CreateCategoryRequest): Promise<Category> {
  const { data } = await productApi.post<Category>('/categories', payload);
  return data;
}

export async function updateCategory(id: string, payload: CreateCategoryRequest): Promise<Category> {
  const { data } = await productApi.put<Category>(`/categories/${id}`, payload);
  return data;
}

export async function deleteCategory(id: string): Promise<void> {
  await productApi.delete(`/categories/${id}`);
}
