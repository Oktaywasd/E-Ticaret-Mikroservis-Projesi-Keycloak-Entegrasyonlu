import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import {
  ShoppingCart, ChevronRight, Package, Truck, Shield, RotateCcw, Minus, Plus,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { ProductDetailSkeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { useProduct } from './useProductQueries';
import { useCartStore } from '@/features/cart/cartStore';
import { StarRating } from '@/components/common/StarRating';
import { ProductImageGallery } from '@/components/product/ProductImageGallery';
import { ProductDiscussionTabs } from '@/components/product/ProductDiscussionTabs';
import type { ProductVariant } from './types';

export function ProductDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { data: product, isLoading, isError, error, refetch } = useProduct(id!);
  const items = useCartStore((s) => s.items);
  const addItem = useCartStore((s) => s.addItem);

  const [selectedVariant, setSelectedVariant] = useState<ProductVariant | null>(null);
  const [quantity, setQuantity] = useState(1);

  if (isLoading) return <ProductDetailSkeleton />;
  if (isError || !product) return <ErrorMessage error={error} onRetry={refetch} />;

  const basePrice = product.price.sellingPrice;
  const effectivePrice = basePrice + (selectedVariant?.additionalPrice ?? 0);
  const effectiveStock = selectedVariant?.stock ?? product.stock.currentStock;

  const variantString = selectedVariant ? `${selectedVariant.name}: ${selectedVariant.value}` : undefined;
  const currentInCart = items.find(i => i.productId === product?.id && i.variant === variantString)?.quantity ?? 0;
  const remainingStock = effectiveStock - currentInCart;

  const formatPrice = (val: number) =>
    new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(val);

  const handleAddToCart = () => {
    addItem({
      productId: product.id,
      name: product.name,
      price: effectivePrice,
      imageUrl: product.imageUrl,
      quantity,
      variant: variantString,
      stock: effectiveStock,
    });
  };

  // Group variants by name (e.g. Renk, Beden)
  const variantGroups = product.variants?.reduce<Record<string, ProductVariant[]>>((acc, v) => {
    if (!acc[v.name]) acc[v.name] = [];
    acc[v.name].push(v);
    return acc;
  }, {}) ?? {};

  return (
    <div className="container mx-auto px-4 py-8">
      {/* Breadcrumb */}
      <nav className="flex items-center gap-2 text-sm text-muted-foreground mb-6" aria-label="Breadcrumb">
        <Link to="/" className="hover:text-foreground transition-colors">Ana Sayfa</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <Link to="/products" className="hover:text-foreground transition-colors">Ürünler</Link>
        <ChevronRight className="h-3.5 w-3.5" />
        <span className="text-foreground truncate max-w-[200px]">{product.name}</span>
      </nav>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12">
        {/* ── Images ─────────────────────────────────────── */}
        <div className="space-y-3 relative">
          <ProductImageGallery images={product.imageUrls} />
          {product.stock.currentStock === 0 && (
            <div className="absolute inset-0 bg-black/60 flex items-center justify-center rounded-xl z-10 pointer-events-none">
              <Badge variant="destructive" className="text-sm px-4 py-2">Stokta Yok</Badge>
            </div>
          )}
        </div>

        {/* ── Info ────────────────────────────────────────── */}
        <div className="space-y-5">
          {/* Category + Brand */}
          <div className="flex items-center gap-2 flex-wrap">
            <Badge variant="secondary">{product.category?.name}</Badge>
            {product.brand && <Badge variant="outline">{product.brand}</Badge>}
            {!product.active && product.stock.currentStock === 0 && <Badge variant="destructive">Pasif</Badge>}
          </div>

          {/* Name */}
          <h1 className="text-2xl font-bold leading-snug">{product.name}</h1>

          {/* Rating */}
          <div className="flex items-center gap-2">
            <StarRating rating={product.ratingAverage} reviewCount={product.reviewCount} />
          </div>

          {/* Price */}
          <div className="py-4 border-y border-border/50 flex flex-col gap-1">
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold text-violet-400">
                {formatPrice(effectivePrice)}
              </span>
              {selectedVariant?.additionalPrice !== undefined && selectedVariant.additionalPrice !== 0 && (
                <span className="ml-2 text-sm text-muted-foreground">
                  (Ek ücret: +{formatPrice(selectedVariant.additionalPrice)})
                </span>
              )}
            </div>
          </div>

          {/* Variants */}
          {Object.entries(variantGroups).map(([groupName, variants]) => (
            <div key={groupName} className="space-y-2">
              <p className="text-sm font-medium">
                {groupName}
                {selectedVariant?.name === groupName && (
                  <span className="ml-2 text-muted-foreground font-normal">
                    {selectedVariant.value}
                  </span>
                )}
              </p>
              <div className="flex flex-wrap gap-2">
                {variants.map((v) => (
                  <button
                    key={`${v.name}-${v.value}`}
                    onClick={() => setSelectedVariant(v.value === selectedVariant?.value ? null : v)}
                    className={`rounded-lg border px-3 py-1.5 text-sm font-medium transition-all ${
                      selectedVariant?.value === v.value && selectedVariant.name === v.name
                        ? 'border-violet-500 bg-violet-500/20 text-violet-300'
                        : 'border-border/50 hover:border-violet-500/50'
                    } ${(v.stock !== undefined && v.stock === 0) ? 'opacity-40 cursor-not-allowed line-through' : ''}`}
                    disabled={v.stock !== undefined && v.stock === 0}
                    aria-label={`${groupName}: ${v.value}`}
                  >
                    {v.value}
                  </button>
                ))}
              </div>
            </div>
          ))}

          {/* Stock */}
          <div className="flex items-center gap-2">
            <div
              className={`h-2 w-2 rounded-full ${
                effectiveStock === 0
                  ? 'bg-red-500'
                  : effectiveStock <= 5
                  ? 'bg-amber-500'
                  : 'bg-emerald-500'
              }`}
            />
            <span className="text-sm text-muted-foreground">
              {effectiveStock === 0
                ? 'Stokta yok'
                : effectiveStock <= 5
                ? `Son ${effectiveStock} adet`
                : `${effectiveStock} adet stokta`}
            </span>
          </div>

          {/* Quantity + Add to Cart */}
          <div className="flex items-center gap-3">
            <div className="flex items-center border border-border/50 rounded-lg overflow-hidden">
              <Button
                variant="ghost"
                size="icon"
                className="h-10 w-10 rounded-none border-r border-border/50"
                onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                disabled={quantity <= 1 || remainingStock <= 0}
                aria-label="Azalt"
              >
                <Minus className="h-4 w-4" />
              </Button>
              <span className="w-10 text-center text-sm font-semibold">{quantity}</span>
              <Button
                variant="ghost"
                size="icon"
                className="h-10 w-10 rounded-none border-l border-border/50"
                onClick={() => setQuantity((q) => Math.min(remainingStock, q + 1))}
                disabled={quantity >= remainingStock || remainingStock <= 0}
                aria-label="Artır"
              >
                <Plus className="h-4 w-4" />
              </Button>
            </div>

            <Button
              id="add-to-cart-detail"
              className="flex-1 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 shadow-lg shadow-violet-500/20"
              disabled={remainingStock <= 0 || effectiveStock === 0}
              onClick={handleAddToCart}
            >
              <ShoppingCart className="h-4 w-4 mr-2" />
              {effectiveStock === 0 ? 'Stokta Yok' : remainingStock <= 0 ? 'Tüm stok sepetinizde' : 'Sepete Ekle'}
            </Button>
          </div>

          {/* Guarantees */}
          <div className="grid grid-cols-3 gap-3 pt-2">
            {[
              { icon: <Truck className="h-4 w-4" />, label: 'Ücretsiz Kargo' },
              { icon: <Shield className="h-4 w-4" />, label: 'Güvenli Ödeme' },
              { icon: <RotateCcw className="h-4 w-4" />, label: '30 Gün İade' },
            ].map((g) => (
              <div
                key={g.label}
                className="flex flex-col items-center gap-1.5 rounded-lg border border-border/50 p-3 text-center"
              >
                <span className="text-violet-400">{g.icon}</span>
                <span className="text-xs text-muted-foreground">{g.label}</span>
              </div>
            ))}
          </div>

          {/* Description */}
          {product.description && (
            <div className="space-y-2 pt-2">
              <h2 className="font-semibold">Ürün Açıklaması</h2>
              <p className="text-sm text-muted-foreground leading-relaxed whitespace-pre-line">
                {product.description}
              </p>
            </div>
          )}

          {/* Product Code */}
          <p className="text-xs text-muted-foreground/60">
            Ürün Kodu: <span className="font-mono">{product.productCode}</span>
          </p>
        </div>
      </div>

      {product?.id && (
        <ProductDiscussionTabs productId={product.id} />
      )}
    </div>
  );
}
