// ─── Category ─────────────────────────────────────────────────────────────────
export interface Category {
  id: string;
  name: string;
  description?: string;
  parentId?: string;
  createdAt?: string;
  updatedAt?: string;
}

// ─── Product Variant ──────────────────────────────────────────────────────────
export interface ProductVariant {
  name: string;   // e.g. "Renk", "Beden"
  value: string;  // e.g. "Kırmızı", "XL"
  additionalPrice?: number;
  stock?: number;
  sku?: string;
}

// ─── Product ──────────────────────────────────────────────────────────────────
export interface Product {
  id: string;
  productCode: string;
  name: string;
  description?: string;
  price: {
    sellingPrice: number;
    discountedPrice: number;
  };
  stock: {
    currentStock: number;
    minimumStock: number;
  };
  category: Category;
  brand?: string;
  imageUrl?: string;
  images?: string[];
  imageUrls?: string[];
  ratingAverage?: number;
  reviewCount?: number;
  variants?: ProductVariant[];
  isActive: boolean;
  active?: boolean; // fallback
  deleted?: boolean;
  createdAt: string;
  updatedAt: string;
}

// ─── Product Suggestion ───────────────────────────────────────────────────────
export interface ProductSuggestion {
  id: string;
  name: string;
  brand: string;
  price: number;
  imageUrl?: string;
}

// ─── Request DTOs ─────────────────────────────────────────────────────────────
export interface CreateProductRequest {
  productCode: string;
  name: string;
  description?: string;
  price: number;
  stock: number;
  categoryId: string;
  brand?: string;
  variants?: ProductVariant[];
}

export interface UpdateProductRequest extends Partial<CreateProductRequest> {}

export interface StockUpdateRequest {
  stock: number;
}

export interface CreateCategoryRequest {
  name: string;
  description?: string;
  parentId?: string;
}

// ─── Query Params ─────────────────────────────────────────────────────────────
export interface ProductQueryParams {
  page?: number;
  size?: number;
  search?: string;
  categoryId?: string;
  brand?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  sortDirection?: string;
  sort?: string;
  includeDeleted?: boolean;
  includeInactive?: boolean;
}
