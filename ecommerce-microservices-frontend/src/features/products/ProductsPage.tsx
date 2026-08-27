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
import { useProducts, useCategories } from './useProductQueries';
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

  // URL is the source of truth for active filters
  const activeFilters = useMemo<ProductQueryParams>(() => {
    const minP = searchParams.get('minPrice');
    const maxP = searchParams.get('maxPrice');
    return {
      page: Number(searchParams.get('page') || 0),
      size: PAGE_SIZE,
      search: searchParams.get('search') || undefined,
      categoryId: searchParams.get('categoryId') || undefined,
      brand: searchParams.get('brand') || undefined,
      sort: searchParams.get('sort') || undefined,
      minPrice: minP ? Number(minP) : undefined,
      maxPrice: maxP ? Number(maxP) : undefined,
    };
  }, [searchParams]);

  // Local form state for the sidebar
  const [filterForm, setFilterForm] = useState({
    search: activeFilters.search ?? '',
    categoryId: activeFilters.categoryId ?? '',
    brand: activeFilters.brand ?? '',
    sort: activeFilters.sort ?? '',
    minPrice: activeFilters.minPrice ?? '',
    maxPrice: activeFilters.maxPrice ?? '',
  });

  // Sync form with URL when URL changes (e.g. from header search or initial load)
  useEffect(() => {
    setFilterForm({
      search: activeFilters.search ?? '',
      categoryId: activeFilters.categoryId ?? '',
      brand: activeFilters.brand ?? '',
      sort: activeFilters.sort ?? '',
      minPrice: activeFilters.minPrice ?? '',
      maxPrice: activeFilters.maxPrice ?? '',
    });
  }, [activeFilters]);

  const { data, isLoading, isError, error, refetch } = useProducts(activeFilters);
  const { data: categories } = useCategories();

  const handleApplyFilters = () => {
    const params: Record<string, string> = {};
    
    // Always reset page to 0 on apply
    params.page = '0';
    
    if (filterForm.search) params.search = filterForm.search;
    if (filterForm.categoryId) params.categoryId = filterForm.categoryId;
    if (filterForm.brand) params.brand = filterForm.brand;
    if (filterForm.sort) params.sort = filterForm.sort;
    if (filterForm.minPrice !== '' && filterForm.minPrice !== null) {
      params.minPrice = String(Number(filterForm.minPrice));
    }
    if (filterForm.maxPrice !== '' && filterForm.maxPrice !== null) {
      params.maxPrice = String(Number(filterForm.maxPrice));
    }
    
    setSearchParams(params, { replace: true });
    if (sidebarOpen) setSidebarOpen(false);
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    handleApplyFilters();
  };

  const handleClearFilters = () => {
    setSearchParams({ page: '0' }, { replace: true });
  };

  const updateFilterForm = (updates: Partial<typeof filterForm>) => {
    setFilterForm(prev => ({ ...prev, ...updates }));
  };

  // Pagination & Data (Server-Side)
  const totalElements = data?.totalElements || 0;
  const totalPages = data?.totalPages || 0;
  const currentPage = data?.number || 0;
  const currentProducts = data?.content || [];

  const hasActiveFilters = !!(
    activeFilters.search || 
    activeFilters.categoryId || 
    activeFilters.brand || 
    activeFilters.minPrice || 
    activeFilters.maxPrice
  );

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
                placeholder="Ürün adı veya marka…"
                value={filterForm.search}
                onChange={(e) => updateFilterForm({ search: e.target.value })}
                className="pl-9"
              />
            </div>
          </form>

          {/* Category */}
          <div className="space-y-1.5">
            <label className="text-sm font-medium">Kategori</label>
            <Select
              id="filter-category"
              value={filterForm.categoryId}
              onChange={(e) => updateFilterForm({ categoryId: e.target.value })}
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
              value={filterForm.sort}
              onChange={(e) => updateFilterForm({ sort: e.target.value })}
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
                value={filterForm.minPrice}
                onChange={(e) => updateFilterForm({ minPrice: e.target.value })}
              />
              <span className="text-muted-foreground">–</span>
              <Input
                id="filter-max-price"
                type="number"
                placeholder="Max"
                min={0}
                value={filterForm.maxPrice}
                onChange={(e) => updateFilterForm({ maxPrice: e.target.value })}
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
              value={filterForm.brand}
              onChange={(e) => updateFilterForm({ brand: e.target.value })}
            />
          </div>

          {/* Apply and Clear buttons */}
          <div className="flex flex-col gap-2 pt-2">
            <Button size="sm" onClick={handleApplyFilters} className="w-full">
              Filtreleri Uygula
            </Button>
            
            {hasActiveFilters && (
              <Button variant="outline" size="sm" onClick={handleClearFilters} className="w-full">
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
                <Button variant="outline" size="sm" onClick={handleClearFilters}>
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
                onPageChange={(page) => {
                  const newParams = new URLSearchParams(searchParams);
                  newParams.set('page', String(page));
                  setSearchParams(newParams, { replace: true });
                }}
              />
            </>
          )}
        </div>
      </div>
    </div>
  );
}
