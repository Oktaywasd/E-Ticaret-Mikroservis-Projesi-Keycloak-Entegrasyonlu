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
} from './types';

// ─── Products ─────────────────────────────────────────────────────────────────

export async function fetchProducts(params: ProductQueryParams = {}): Promise<PagedResponse<Product>> {
  const { data } = await productApi.get<PagedResponse<Product>>('/products', { params });
  return data;
}

export async function fetchProductsByCategory(categoryId: string, params: ProductQueryParams = {}): Promise<PagedResponse<Product>> {
  const { data } = await productApi.get<PagedResponse<Product>>(`/products/category/${categoryId}`, { params });
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

export async function updateProductStock(id: string, payload: StockUpdateRequest): Promise<Product> {
  const { data } = await productApi.put<Product>(`/products/${id}/reduce-stock`, payload);
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
