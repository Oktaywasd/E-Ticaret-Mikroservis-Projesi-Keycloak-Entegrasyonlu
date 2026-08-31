// @ts-nocheck
import { useState, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import {
  Plus, Search, Edit2, Trash2, Package, Archive, X
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { Pagination } from '@/components/ui/pagination';
import { DeleteConfirmModal } from '@/components/ui/delete-confirm-modal';
import { StockUpdateModal } from './StockUpdateModal';
import { useProducts, useDeleteProduct, useCategories, useToggleProductStatus, productKeys } from './useProductQueries';
import type { Product } from './types';

const PAGE_SIZE = 50; // Increased to allow better local filtering coverage

export function AdminProductsPage() {
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const categoryId = searchParams.get('categoryId') || undefined;
  const categoryName = searchParams.get('categoryName') || '';

  const [page, setPage] = useState(0);
  const [searchTerm, setSearchTerm] = useState('');
  const [includeDeleted, setIncludeDeleted] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState<Product | null>(null);
  const [stockTarget, setStockTarget] = useState<Product | null>(null);

  const { data, isLoading, isError, error, refetch } = useProducts({
    page,
    size: PAGE_SIZE,
    includeDeleted,
    includeInactive: true,
    categoryId,
  });
  
  const { data: categories } = useCategories();

  const products = data?.content || [];

  const categoryMap = useMemo(() => {
    const map: Record<string, string> = {};
    if (categories) {
      categories.forEach((cat: any) => {
        map[cat.id] = cat.name || cat.title;
      });
    }
    return map;
  }, [categories]);

  const filteredProducts = useMemo(() => {
    if (!searchTerm.trim()) return products;
    const query = searchTerm.toLowerCase().trim();
    return products.filter((p) => {
      const catName = p.categoryName || p.category?.name || categoryMap[p.categoryId] || '';
      return p.name?.toLowerCase().includes(query) ||
             p.productCode?.toLowerCase().includes(query) ||
             catName.toLowerCase().includes(query);
    });
  }, [products, searchTerm, categoryMap]);

  const { mutate: deleteProduct, isPending: deleting } = useDeleteProduct();
  const { mutateAsync: toggleStatusAsync } = useToggleProductStatus();
  
  const [togglingIds, setTogglingIds] = useState<Set<string>>(new Set());

  const qc = useQueryClient();

  const handleToggle = async (e: React.MouseEvent, productId: string) => {
    e.preventDefault();
    e.stopPropagation(); // prevent bubbling

    if (togglingIds.has(productId)) return; // prevent double clicks

    setTogglingIds(prev => {
      const next = new Set(prev);
      next.add(productId);
      return next;
    });

    try {
      // 1. Backend'e PATCH isteği at
      await toggleStatusAsync(productId);
      
      // 2. React Query cache'ini yenile ve sunucudaki güncel durumu anında çek
      await refetch();
      qc.invalidateQueries({ queryKey: productKeys.lists() });
      qc.invalidateQueries({ queryKey: ['products'] });
    } catch (error) {
      console.error("Durum güncellenirken hata:", error);
    } finally {
      setTogglingIds(prev => {
        const next = new Set(prev);
        next.delete(productId);
        return next;
      });
    }
  };

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
  };

  const clearSearch = () => {
    setSearchTerm('');
  };

  const handleDelete = () => {
    if (!deleteTarget) return;
    deleteProduct(deleteTarget.id, {
      onSuccess: () => setDeleteTarget(null),
    });
  };

  const formatPrice = (price: any) => {
    if (price === null || price === undefined) return "₺0,00";
    if (typeof price === 'object') {
      const val = price.sellingPrice ?? price.amount ?? price.originalPrice ?? 0;
      return `₺${Number(val).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`;
    }
    return `₺${Number(price).toLocaleString('tr-TR', { minimumFractionDigits: 2 })}`;
  };

  return (
    <div className="space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div>
          <h1 className="text-xl font-bold">Ürün Yönetimi</h1>
          <p className="text-sm text-muted-foreground">{filteredProducts.length} ürün listeleniyor</p>
        </div>
        <Button
          id="add-product-button"
          className="bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700"
          onClick={() => navigate('/admin/products/new')}
        >
          <Plus className="h-4 w-4 mr-2" />
          Yeni Ürün
        </Button>
      </div>

      {/* Toolbar */}
      <div className="flex flex-col gap-3">
        {categoryId && (
          <div className="flex items-center">
            <Badge variant="outline" className="bg-violet-500/10 text-violet-500 border-violet-500/20 px-3 py-1 flex items-center gap-2">
              Kategori: {categoryName || categoryMap[categoryId] || 'Bilinmeyen'}
              <button
                type="button"
                onClick={() => {
                  searchParams.delete('categoryId');
                  searchParams.delete('categoryName');
                  setSearchParams(searchParams);
                }}
                className="hover:text-violet-700 transition-colors"
                aria-label="Kategori filtresini temizle"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            </Badge>
          </div>
        )}
        <div className="flex flex-col sm:flex-row gap-3">
          <form onSubmit={handleSearch} className="flex gap-2 flex-1 relative">
            <div className="relative flex-1">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                id="admin-products-search"
                placeholder="Ürün adı, stok kodu veya kategori ara…"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9 pr-10"
              />
              {searchTerm && (
                <button
                  type="button"
                  onClick={clearSearch}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                >
                  <X className="h-4 w-4" />
                </button>
              )}
            </div>
            <Button type="submit" variant="outline" size="sm">Ara</Button>
          </form>
        </div>
      </div>

      {/* Table */}
      {isError ? (
        <ErrorMessage error={error} onRetry={refetch} />
      ) : (
        <div className="rounded-xl border border-border/50 overflow-hidden">
          <table className="w-full text-sm">
            <thead className="border-b border-border/50 bg-muted/30">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Ürün</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground hidden sm:table-cell">Kategori</th>
                <th className="px-4 py-3 text-right font-medium text-muted-foreground">Fiyat</th>
                <th className="px-4 py-3 text-right font-medium text-muted-foreground">Stok</th>
                <th className="px-4 py-3 text-center font-medium text-muted-foreground">Durum</th>
                <th className="px-4 py-3 text-right font-medium text-muted-foreground">İşlem</th>
              </tr>
            </thead>
            <tbody>
              {isLoading
                ? Array.from({ length: PAGE_SIZE }).map((_, i) => (
                    <tr key={i} className="border-b border-border/30">
                      <td className="px-4 py-3"><Skeleton className="h-9 w-full" /></td>
                      <td className="px-4 py-3 hidden sm:table-cell"><Skeleton className="h-4 w-20" /></td>
                      <td className="px-4 py-3"><Skeleton className="h-4 w-16 ml-auto" /></td>
                      <td className="px-4 py-3"><Skeleton className="h-4 w-10 ml-auto" /></td>
                      <td className="px-4 py-3 flex justify-center"><Skeleton className="h-5 w-10 rounded-full" /></td>
                      <td className="px-4 py-3"><Skeleton className="h-8 w-20 ml-auto" /></td>
                    </tr>
                  ))
                : !filteredProducts.length
                ? (
                    <tr>
                      <td colSpan={6} className="px-4 py-12 text-center text-muted-foreground">
                        <div className="flex flex-col items-center gap-3">
                          <Package className="h-8 w-8 text-muted-foreground/40" />
                          <p>{searchTerm ? 'Aramanızla eşleşen ürün bulunamadı.' : 'Ürün bulunamadı'}</p>
                        </div>
                      </td>
                    </tr>
                  )
                : filteredProducts.map((product) => {
                    const isChecked = Boolean(product.isActive ?? (product as any).active ?? false);
                    return (
                    <tr
                      key={product.id}
                      className={`border-b border-border/30 hover:bg-muted/20 transition-colors ${
                        product.deleted ? 'opacity-50' : ''
                      }`}
                    >
                      {/* Product */}
                      <td className="px-4 py-3">
                        <div className="flex items-center gap-3">
                          <div className="h-9 w-9 rounded-lg bg-muted overflow-hidden shrink-0">
                            {product.imageUrl ? (
                              <img src={product.imageUrl} alt="" className="h-full w-full object-cover" />
                            ) : (
                              <div className="flex h-full w-full items-center justify-center">
                                <Package className="h-4 w-4 text-muted-foreground/40" />
                              </div>
                            )}
                          </div>
                          <div className="min-w-0 flex flex-col gap-1">
                            <p className="font-medium truncate max-w-[140px] sm:max-w-[200px] flex items-center gap-2">
                              {product.name}
                              {!isChecked && (
                                <span className="inline-flex items-center px-1.5 py-0.5 rounded text-[10px] font-semibold bg-destructive/10 text-destructive border border-destructive/20 uppercase tracking-wider">
                                  Pasif
                                </span>
                              )}
                            </p>
                            <p className="text-xs text-muted-foreground font-mono">{product.productCode}</p>
                          </div>
                        </div>
                      </td>

                      {/* Category */}
                      <td className="px-4 py-3 hidden sm:table-cell">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-violet-500/10 text-violet-500 border border-violet-500/20">
                          {product.categoryName || product.category?.name || categoryMap[product.categoryId] || 'Kategorisiz'}
                        </span>
                      </td>

                      {/* Price */}
                      <td className="px-4 py-3 text-right font-semibold">
                        {formatPrice(product.price)}
                      </td>

                      {/* Stock */}
                      <td className="px-4 py-3 text-right">
                        <button
                          className="font-semibold hover:text-violet-400 transition-colors"
                          onClick={() => setStockTarget(product)}
                          title="Stok güncelle"
                          aria-label={`${product.name} stok güncelle`}
                        >
                          <span className={
                            product.stock.currentStock === 0
                              ? 'text-destructive'
                              : product.stock.currentStock <= 5
                              ? 'text-amber-400'
                              : ''
                          }>
                            {product.stock.currentStock}
                          </span>
                        </button>
                      </td>

                      {/* Status */}
                      <td className="px-4 py-3 text-center">
                        <button
                          type="button"
                          onClick={(e) => handleToggle(e, product.id)}
                          disabled={togglingIds.has(product.id)}
                          className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer items-center rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none focus-visible:ring-2 focus-visible:ring-violet-500 focus-visible:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed ${
                            isChecked ? 'bg-emerald-500' : 'bg-slate-700'
                          }`}
                          role="switch"
                          aria-checked={isChecked}
                          title={isChecked ? 'Pasife Al' : 'Aktifleştir'}
                        >
                          <span className="sr-only">Durum Değiştir</span>
                          <span
                            aria-hidden="true"
                            className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out ${
                              isChecked ? 'translate-x-5' : 'translate-x-0'
                            }`}
                          />
                        </button>
                      </td>

                      {/* Actions */}
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            asChild
                            aria-label="Düzenle"
                          >
                            <Link to={`/admin/products/${product.id}/edit`}>
                              <Edit2 className="h-3.5 w-3.5" />
                            </Link>
                          </Button>
                          {!product.deleted && (
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-destructive hover:text-destructive hover:bg-destructive/10"
                              onClick={() => setDeleteTarget(product)}
                              aria-label="Sil"
                            >
                              <Trash2 className="h-3.5 w-3.5" />
                            </Button>
                          )}
                        </div>
                      </td>
                    </tr>
                    );
                  })
              }
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <Pagination
          currentPage={data.number}
          totalPages={data.totalPages}
          totalElements={data.totalElements}
          pageSize={data.size}
          onPageChange={setPage}
        />
      )}

      {/* Modals */}
      <DeleteConfirmModal
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title="Ürünü Sil"
        description={`"${deleteTarget?.name}" ürünü silinecek. Bu işlem geri alınamaz.`}
        onConfirm={handleDelete}
        isLoading={deleting}
      />

      {stockTarget && (
        <StockUpdateModal
          open={!!stockTarget}
          onOpenChange={(open) => !open && setStockTarget(null)}
          productId={stockTarget.id}
          productName={stockTarget.name}
          currentStock={stockTarget.stock.currentStock}
        />
      )}
    </div>
  );
}