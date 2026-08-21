import { useCartStore } from './cartStore';
import { Button } from '@/components/ui/button';
import { Trash2, Plus, Minus, ShoppingBag } from 'lucide-react';
import { Link } from 'react-router-dom';

export function CartPage() {
  const { items, removeItem, updateQuantity, totalPrice } = useCartStore();

  if (items.length === 0) {
    return (
      <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 text-center px-4">
        <ShoppingBag className="h-16 w-16 text-muted-foreground/40" />
        <h1 className="text-2xl font-bold">Sepetiniz Boş</h1>
        <p className="text-muted-foreground">Alışverişe başlamak için ürünleri inceleyin.</p>
        <Button asChild>
          <Link to="/products">Ürünleri İncele</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold mb-6">Sepetim</h1>
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-3">
          {items.map((item) => (
            <div
              key={`${item.productId}-${item.variant}`}
              className="flex gap-4 rounded-xl border border-border/50 bg-card p-4"
            >
              <div className="h-20 w-20 rounded-lg bg-muted shrink-0 overflow-hidden">
                {item.imageUrl ? (
                  <img src={item.imageUrl} alt={item.name} className="h-full w-full object-cover" />
                ) : (
                  <div className="flex h-full w-full items-center justify-center">
                    <ShoppingBag className="h-6 w-6 text-muted-foreground" />
                  </div>
                )}
              </div>
              <div className="flex flex-1 flex-col gap-1">
                <p className="font-semibold">{item.name}</p>
                {item.variant && <p className="text-xs text-muted-foreground">{item.variant}</p>}
                <p className="text-violet-400 font-bold">
                  {(item.price * item.quantity).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  variant="outline"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => updateQuantity(item.productId, item.quantity - 1, item.variant)}
                >
                  <Minus className="h-3 w-3" />
                </Button>
                <span className="w-6 text-center text-sm font-semibold">{item.quantity}</span>
                <Button
                  variant="outline"
                  size="icon"
                  className="h-7 w-7"
                  onClick={() => updateQuantity(item.productId, item.quantity + 1, item.variant)}
                  disabled={item.stock !== undefined && item.quantity >= item.stock}
                >
                  <Plus className="h-3 w-3" />
                </Button>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-7 w-7 text-destructive hover:text-destructive ml-2"
                  onClick={() => removeItem(item.productId, item.variant)}
                >
                  <Trash2 className="h-3 w-3" />
                </Button>
              </div>
            </div>
          ))}
        </div>

        {/* Order Summary */}
        <div className="rounded-xl border border-border/50 bg-card p-6 h-fit space-y-4">
          <h2 className="font-semibold text-lg">Sipariş Özeti</h2>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Ara Toplam</span>
            <span>{totalPrice().toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}</span>
          </div>
          <div className="flex justify-between text-sm">
            <span className="text-muted-foreground">Kargo</span>
            <span className="text-emerald-400">Ücretsiz</span>
          </div>
          <div className="border-t border-border/50 pt-4 flex justify-between font-bold">
            <span>Toplam</span>
            <span className="text-violet-400">
              {totalPrice().toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
            </span>
          </div>
          <Button
            id="checkout-button"
            className="w-full bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700"
            asChild
          >
            <Link to="/checkout">Siparişi Tamamla</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
