import { useState, useEffect, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Search, SlidersHorizontal, X, LayoutGrid, LayoutList } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Select } from '@/components/ui/select-native';
import { Pagination } from '@/components/ui/pagination';
import { ProductCard } from './ProductCard';
import { ProductCardSkeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { useProducts, useProductsByCategory, useCategories } from './useProductQueries';
import type { ProductQueryParams } from './types';
import { Package } from 'lucide-react';

const PAGE_SIZE = 12;

const SORT_OPTIONS = [
  { label: 'Varsayılan', value: '' },
  { label: 'En Yeni', value: 'createdAt,desc' },
  { label: 'En Eski', value: 'createdAt,asc' },
  { label: 'Fiyat: Düşükten Yükseğe', value: 'price,asc' },
  { label: 'Fiyat: Yüksekten Düşüğe', value: 'price,desc' },
  { label: 'A–Z', value: 'name,asc' },
  { label: 'Z–A', value: 'name,desc' },
];

export function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [gridView, setGridView] = useState<'grid' | 'list'>('grid');

  // Sync state with URL params
  const [filters, setFilters] = useState<ProductQueryParams>({
    page: Number(searchParams.get('page') ?? 0),
    size: PAGE_SIZE,
    name: searchParams.get('search') ?? undefined,
    categoryId: searchParams.get('categoryId') ?? undefined,
    brand: searchParams.get('brand') ?? undefined,
    sort: searchParams.get('sort') ?? undefined,
    minPrice: searchParams.get('minPrice') ? Number(searchParams.get('minPrice')) : undefined,
    maxPrice: searchParams.get('maxPrice') ? Number(searchParams.get('maxPrice')) : undefined,
  });

  const [draftFilters, setDraftFilters] = useState<ProductQueryParams>(filters);

  // Fetch data only by category (fetch all matching category, no other filters)
  const allProductsQuery = useProducts({ size: 1000 });
  const categoryProductsQuery = useProductsByCategory(filters.categoryId || '', { size: 1000 });

  const activeQuery = filters.categoryId ? categoryProductsQuery : allProductsQuery;
  const { data, isLoading, isError, error, refetch } = activeQuery;
  
  const { data: categories } = useCategories();

  // Keep URL in sync with active filters
  useEffect(() => {
    const params: Record<string, string> = {};
    if (filters.page) params.page = String(filters.page);
    if (filters.name) params.search = filters.name;
    if (filters.categoryId) params.categoryId = filters.categoryId;
    if (filters.brand) params.brand = filters.brand;
    if (filters.sort) params.sort = filters.sort;
    if (filters.minPrice) params.minPrice = String(filters.minPrice);
    if (filters.maxPrice) params.maxPrice = String(filters.maxPrice);
    setSearchParams(params, { replace: true });
  }, [filters, setSearchParams]);

  const updateDraftFilter = (updates: Partial<ProductQueryParams>) => {
    setDraftFilters((prev) => ({ ...prev, ...updates }));
  };

  const applyFilters = () => {
    setFilters({ ...draftFilters, page: 0 });
    if (sidebarOpen) setSidebarOpen(false);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    applyFilters();
  };

  const clearFilters = () => {
    const reset = { page: 0, size: PAGE_SIZE };
    setDraftFilters(reset);
    setFilters(reset);
  };

  // ─── Client-Side Filtering & Sorting ────────────────────────
  const filteredProducts = useMemo(() => {
    if (!data?.content) return [];
    let result = [...data.content];

    // 1. Search (name or description, case-insensitive)
    if (filters.name) {
      const q = filters.name.toLowerCase();
      result = result.filter(
        (p) =>
          p.name.toLowerCase().includes(q) ||
          (p.description && p.description.toLowerCase().includes(q))
      );
    }

    // 2. Brand
    if (filters.brand) {
      const b = filters.brand.toLowerCase();
      result = result.filter((p) => p.brand && p.brand.toLowerCase().includes(b));
    }

    // 3. Price Range (discountedPrice if exists, else sellingPrice)
    if (filters.minPrice !== undefined || filters.maxPrice !== undefined) {
      result = result.filter((p) => {
        const pPrice = p.price?.discountedPrice ?? p.price?.sellingPrice ?? 0;
        if (filters.minPrice !== undefined && pPrice < filters.minPrice) return false;
        if (filters.maxPrice !== undefined && pPrice > filters.maxPrice) return false;
        return true;
      });
    }

    // 4. Sorting
    if (filters.sort) {
      result.sort((a, b) => {
        switch (filters.sort) {
          case 'createdAt,desc': {
            const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
            const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
            return dateB - dateA;
          }
          case 'createdAt,asc': {
            const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
            const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
            return dateA - dateB;
          }
          case 'price,asc': {
            const priceA = a.price?.discountedPrice ?? a.price?.sellingPrice ?? 0;
            const priceB = b.price?.discountedPrice ?? b.price?.sellingPrice ?? 0;
            return priceA - priceB;
          }
          case 'price,desc': {
            const priceA = a.price?.discountedPrice ?? a.price?.sellingPrice ?? 0;
            const priceB = b.price?.discountedPrice ?? b.price?.sellingPrice ?? 0;
            return priceB - priceA;
          }
          case 'name,asc':
            return a.name.localeCompare(b.name);
          case 'name,desc':
            return b.name.localeCompare(a.name);
          default:
            return 0;
        }
      });
    }

    return result;
  }, [data?.content, filters.name, filters.brand, filters.minPrice, filters.maxPrice, filters.sort]);

  // Pagination (Local)
  const totalElements = filteredProducts.length;
  const totalPages = Math.ceil(totalElements / PAGE_SIZE);
  const currentPage = filters.page ?? 0;
  
  const currentProducts = useMemo(() => {
    const start = currentPage * PAGE_SIZE;
    return filteredProducts.slice(start, start + PAGE_SIZE);
  }, [filteredProducts, currentPage]);

  const hasActiveFilters = !!(filters.name || filters.categoryId || filters.brand || filters.minPrice || filters.maxPrice);

  return (
    <div className="container mx-auto px-4 py-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
        <div>
          <h1 className="text-2xl font-bold">Ürünler</h1>
          {data && (
            <p className="text-sm text-muted-foreground">
              {totalElements} ürün bulundu
            </p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {/* Grid / List toggle */}
          <Button
            variant={gridView === 'grid' ? 'secondary' : 'ghost'}
            size="icon"
            className="h-9 w-9"
            onClick={() => setGridView('grid')}
            aria-label="Grid görünümü"
          >
            <LayoutGrid className="h-4 w-4" />
          </Button>
          <Button
            variant={gridView === 'list' ? 'secondary' : 'ghost'}
            size="icon"
            className="h-9 w-9"
            onClick={() => setGridView('list')}
            aria-label="Liste görünümü"
          >
            <LayoutList className="h-4 w-4" />
          </Button>
          {/* Mobile filter toggle */}
          <Button
            variant="outline"
            size="sm"
            className="md:hidden"
            onClick={() => setSidebarOpen((v) => !v)}
          >
            <SlidersHorizontal className="h-4 w-4 mr-2" />
            Filtreler
            {hasActiveFilters && (
              <span className="ml-2 flex h-5 w-5 items-center justify-center rounded-full bg-violet-600 text-[10px] font-bold">
                !
              </span>
            )}
          </Button>
        </div>
      </div>

      <div className="flex gap-6">
        {/* ── Sidebar / Filters ──────────────────────────── */}
        <aside
          className={`
            ${sidebarOpen ? 'flex' : 'hidden'} md:flex
            flex-col w-full md:w-64 shrink-0 gap-5
            ${sidebarOpen ? 'absolute inset-0 z-30 bg-background p-6 overflow-y-auto' : ''}
          `}
        >
          {sidebarOpen && (
            <div className="flex items-center justify-between md:hidden">
              <h2 className="font-semibold">Filtreler</h2>
              <Button variant="ghost" size="icon" onClick={() => setSidebarOpen(false)}>
                <X className="h-4 w-4" />
              </Button>
            </div>
          )}

          {/* Search */}
          <form onSubmit={handleSearch} className="space-y-1.5">
            <label className="text-sm font-medium">Ürün Ara</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                id="products-search"
                type="search"
                placeholder="Ürün adı…"
                value={draftFilters.name ?? ''}
                onChange={(e) => updateDraftFilter({ name: e.target.value || undefined })}
                className="pl-9"
              />
            </div>
          </form>

          {/* Category */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">Kategori</label>
            <Select
              id="filter-category"
              value={draftFilters.categoryId ?? ''}
              onChange={(e) => updateDraftFilter({ categoryId: e.target.value || undefined })}
              placeholder="Tüm Kategoriler"
            >
              {categories?.map((c) => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </Select>
          </div>

          {/* Sort */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">Sıralama</label>
            <Select
              id="filter-sort"
              value={draftFilters.sort ?? ''}
              onChange={(e) => updateDraftFilter({ sort: e.target.value || undefined })}
            >
              {SORT_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>{o.label}</option>
              ))}
            </Select>
          </div>

          {/* Price Range */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">Fiyat Aralığı (₺)</label>
            <div className="flex items-center gap-2">
              <Input
                id="filter-min-price"
                type="number"
                placeholder="Min"
                min={0}
                value={draftFilters.minPrice ?? ''}
                onChange={(e) =>
                  updateDraftFilter({ minPrice: e.target.value ? Number(e.target.value) : undefined })
                }
              />
              <span className="text-muted-foreground">–</span>
              <Input
                id="filter-max-price"
                type="number"
                placeholder="Max"
                min={0}
                value={draftFilters.maxPrice ?? ''}
                onChange={(e) =>
                  updateDraftFilter({ maxPrice: e.target.value ? Number(e.target.value) : undefined })
                }
              />
            </div>
          </div>

          {/* Brand */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">Marka</label>
            <Input
              id="filter-brand"
              type="text"
              placeholder="Marka adı…"
              value={draftFilters.brand ?? ''}
              onChange={(e) => updateDraftFilter({ brand: e.target.value || undefined })}
            />
          </div>

          {/* Apply and Clear buttons */}
          <div className="flex flex-col gap-2 pt-2">
            <Button size="sm" onClick={applyFilters} className="w-full">
              Filtreleri Uygula
            </Button>
            
            {hasActiveFilters && (
              <Button variant="outline" size="sm" onClick={clearFilters} className="w-full">
                <X className="h-4 w-4 mr-2" />
                Filtreleri Temizle
              </Button>
            )}
          </div>
        </aside>

        {/* ── Product Grid ────────────────────────────────── */}
        <div className="flex-1 min-w-0">
          {isError ? (
            <ErrorMessage error={error} onRetry={() => refetch()} />
          ) : isLoading ? (
            <div
              className={
                gridView === 'grid'
                  ? 'grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-4'
                  : 'flex flex-col gap-3'
              }
            >
              {Array.from({ length: PAGE_SIZE }).map((_, i) => (
                <ProductCardSkeleton key={i} />
              ))}
            </div>
          ) : totalElements === 0 ? (
            <div className="flex flex-col items-center justify-center gap-4 py-20 text-center">
              <div className="rounded-full bg-muted p-6">
                <Package className="h-12 w-12 text-muted-foreground/40" />
              </div>
              <p className="font-semibold">Ürün bulunamadı</p>
              <p className="text-sm text-muted-foreground">Filtrelerinizi değiştirerek tekrar deneyin.</p>
              {hasActiveFilters && (
                <Button variant="outline" size="sm" onClick={clearFilters}>
                  Filtreleri Temizle
                </Button>
              )}
            </div>
          ) : (
            <>
              <div
                className={
                  gridView === 'grid'
                    ? 'grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-4 gap-4'
                    : 'flex flex-col gap-3'
                }
              >
                {currentProducts.map((product) => (
                  <ProductCard key={product.id} product={product} />
                ))}
              </div>

              <Pagination
                currentPage={currentPage}
                totalPages={totalPages}
                totalElements={totalElements}
                pageSize={PAGE_SIZE}
                onPageChange={(page) => setFilters((f) => ({ ...f, page }))}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
