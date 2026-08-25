import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ShoppingCart, Package } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { useCartStore } from '@/features/cart/cartStore';
import { StarRating } from '@/components/common/StarRating';
import type { Product } from './types';

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const addItem = useCartStore((s) => s.addItem);
  const [imgError, setImgError] = useState(false);

  const isOutOfStock = product.stock.currentStock === 0;
  const isLowStock = product.stock.currentStock > 0 && product.stock.currentStock <= 5;
  const imageUrl = product.imageUrls?.[0] || product.imageUrl;

  const handleAddToCart = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    addItem({
      productId: product.id,
      name: product.name,
      price: product.price.sellingPrice,
      imageUrl: imageUrl,
      quantity: 1,
      stock: product.stock.currentStock,
    });
  };

  const formatPrice = (val: number) =>
    new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(val);

  return (
    <Link
      to={`/products/${product.id}`}
      id={`product-card-${product.id}`}
      className="group relative flex flex-col rounded-xl border border-border/50 bg-card overflow-hidden hover:border-violet-500/40 hover:shadow-lg hover:shadow-violet-500/10 transition-all duration-300"
    >
      {/* Image */}
      <div className="relative aspect-square overflow-hidden bg-muted/30">
        {imageUrl && !imgError ? (
          <img
            src={imageUrl}
            alt={product.name}
            className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            loading="lazy"
            onError={() => setImgError(true)}
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center">
            <Package className="h-16 w-16 text-muted-foreground/20" />
          </div>
        )}

        {/* Badges */}
        <div className="absolute top-2 left-2 flex flex-col gap-1">
          {isOutOfStock && (
            <Badge variant="destructive" className="text-[10px]">Stokta Yok</Badge>
          )}
          {isLowStock && !isOutOfStock && (
            <Badge variant="warning" className="text-[10px]">Son {product.stock.currentStock} adet</Badge>
          )}
        </div>

        {/* Quick Add overlay */}
        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-end justify-center pb-4">
          <Button
            id={`add-to-cart-${product.id}`}
            size="sm"
            onClick={handleAddToCart}
            disabled={isOutOfStock}
            className="bg-white text-black hover:bg-white/90 font-semibold shadow-lg translate-y-4 group-hover:translate-y-0 transition-transform duration-300 disabled:opacity-50"
          >
            <ShoppingCart className="h-4 w-4 mr-1.5" />
            {isOutOfStock ? 'Stokta Yok' : 'Sepete Ekle'}
          </Button>
        </div>
      </div>

      {/* Info */}
      <div className="flex flex-col flex-1 p-4 gap-2">
        {/* Category / Brand */}
        <div className="flex items-center gap-2">
          <span className="text-xs text-muted-foreground truncate">
            {product.category?.name}
          </span>
          {product.brand && (
            <>
              <span className="text-muted-foreground/40">·</span>
              <span className="text-xs text-muted-foreground truncate">{product.brand}</span>
            </>
          )}
        </div>

        {/* Name */}
        <h3 className="font-semibold text-lg leading-tight line-clamp-2 mb-1" title={product.name}>
          {product.name}
        </h3>
        
        {/* Price */}
        <div className="mt-auto flex flex-col gap-0.5">
          <span className="font-bold text-lg text-violet-700 whitespace-nowrap">
            {formatPrice(product.price.sellingPrice)}
          </span>
        </div>

        {/* Rating */}
        <div className="pt-2 flex items-center justify-end">
          <StarRating rating={product.ratingAverage} reviewCount={product.reviewCount} />
        </div>
      </div>
    </Link>
  );
}
