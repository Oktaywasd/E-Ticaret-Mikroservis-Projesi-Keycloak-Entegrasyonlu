import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { MapPin, CheckCircle2, ShoppingBag, CreditCard, AlertCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { useCartStore } from '@/features/cart/cartStore';
import { useAddresses, useProfile } from '@/features/crm/useCrmQueries';
import { useCreateOrder } from './useOrderQueries';
import type { OrderAddress, CreateOrderRequest } from './types';

export function CheckoutPage() {
  const navigate = useNavigate();
  const { items, totalPrice, clearCart } = useCartStore();
  const { data: addresses, isLoading: loadingAddresses } = useAddresses();
  const { data: profile } = useProfile();
  const createOrder = useCreateOrder();

  const [selectedShippingId, setSelectedShippingId] = useState<string>('');
  const [selectedBillingId, setSelectedBillingId] = useState<string>('');
  const [sameAsShipping, setSameAsShipping] = useState(true);

  // Pre-select first address
  useEffect(() => {
    if (addresses?.length && !selectedShippingId) {
      const def = addresses[0];
      setSelectedShippingId(def.id);
      if (sameAsShipping) setSelectedBillingId(def.id);
    }
  }, [addresses, selectedShippingId, sameAsShipping]);

  if (items.length === 0) {
    return (
      <div className="container mx-auto px-4 py-16 text-center max-w-md">
        <div className="rounded-full bg-muted p-6 w-fit mx-auto mb-4">
          <ShoppingBag className="h-10 w-10 text-muted-foreground/40" />
        </div>
        <h2 className="text-2xl font-bold mb-2">Sepetiniz Boş</h2>
        <p className="text-muted-foreground mb-6">Ödeme adımına geçmek için sepetinize ürün eklemelisiniz.</p>
        <Button asChild className="w-full"><Link to="/products">Alışverişe Başla</Link></Button>
      </div>
    );
  }

  const handleCheckout = () => {
    if (!selectedShippingId || (!sameAsShipping && !selectedBillingId)) return;

    const shipAddr = addresses?.find((a) => a.id === selectedShippingId);
    const billAddr = sameAsShipping ? shipAddr : addresses?.find((a) => a.id === selectedBillingId);

    if (!shipAddr || !billAddr) return;



    const payload: CreateOrderRequest = {
      addressId: shipAddr.id,
      items: items.map((i: any) => ({
        productId: i.id || i.productId,
        quantity: i.quantity,
        price: i.price,
      })),
    };

    createOrder.mutate(payload, {
      onSuccess: (order) => {
        clearCart();
        navigate(`/orders/${order.id}`, { state: { justCreated: true } });
      },
    });
  };

  return (
    <div className="container mx-auto px-4 py-8 max-w-6xl">
      <h1 className="text-2xl font-bold mb-8">Güvenli Ödeme</h1>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Addresses */}
        <div className="lg:col-span-2 space-y-8">
          
          {/* Missing Profile Warning */}
          {profile === undefined && !loadingAddresses && (
             <div className="flex items-start gap-3 rounded-xl border border-amber-500/30 bg-amber-500/10 p-4">
               <AlertCircle className="h-5 w-5 text-amber-400 shrink-0 mt-0.5" />
               <div>
                 <p className="font-semibold text-amber-300">Profiliniz eksik!</p>
                 <p className="text-sm text-amber-400/80 mb-2">Sipariş verebilmek için lütfen profil bilgilerinizi doldurun.</p>
                 <Button size="sm" variant="outline" asChild><Link to="/profile">Profile Git</Link></Button>
               </div>
             </div>
          )}

          {/* Shipping Address */}
          <section className="space-y-4">
            <h2 className="text-xl font-semibold flex items-center gap-2">
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-violet-600 text-xs text-white">1</span>
              Teslimat Adresi
            </h2>

            {loadingAddresses ? (
              <Skeleton className="h-32 w-full" />
            ) : !addresses?.length ? (
              <div className="rounded-xl border border-dashed border-border/50 p-6 text-center">
                <p className="text-muted-foreground mb-3">Kayıtlı adresiniz bulunmuyor.</p>
                <Button variant="outline" asChild><Link to="/profile">Adres Ekle</Link></Button>
              </div>
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                {addresses.map((addr) => (
                  <button
                    key={addr.id}
                    onClick={() => {
                      setSelectedShippingId(addr.id);
                      if (sameAsShipping) setSelectedBillingId(addr.id);
                    }}
                    className={`text-left p-4 rounded-xl border transition-all ${
                      selectedShippingId === addr.id
                        ? 'border-violet-500 bg-violet-500/10'
                        : 'border-border/50 hover:border-violet-500/40 bg-card'
                    }`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <p className="font-semibold text-sm">{addr.addressTitle || addr.title}</p>
                      {selectedShippingId === addr.id && <CheckCircle2 className="h-4 w-4 text-violet-400" />}
                    </div>
                    <p className="text-sm text-foreground">{addr.addressLine || addr.street}</p>
                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{addr.district || addr.state}, {addr.city}</p>
                    <p className="text-xs text-muted-foreground">{addr.country}</p>
                  </button>
                ))}
              </div>
            )}
          </section>

          {/* Billing Address */}
          <section className="space-y-4">
            <h2 className="text-xl font-semibold flex items-center gap-2">
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-violet-600 text-xs text-white">2</span>
              Fatura Adresi
            </h2>

            <label className="flex items-center gap-2 cursor-pointer w-fit">
              <input
                type="checkbox"
                className="h-4 w-4 rounded border-input bg-background checked:bg-violet-600"
                checked={sameAsShipping}
                onChange={(e) => {
                  setSameAsShipping(e.target.checked);
                  if (e.target.checked) setSelectedBillingId(selectedShippingId);
                }}
              />
              <span className="text-sm">Fatura adresim teslimat adresim ile aynı olsun</span>
            </label>

            {!sameAsShipping && addresses && addresses.length > 0 && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-3">
                {addresses.map((addr) => (
                  <button
                    key={addr.id}
                    onClick={() => setSelectedBillingId(addr.id)}
                    className={`text-left p-4 rounded-xl border transition-all ${
                      selectedBillingId === addr.id
                        ? 'border-violet-500 bg-violet-500/10'
                        : 'border-border/50 hover:border-violet-500/40 bg-card'
                    }`}
                  >
                    <div className="flex justify-between items-start mb-2">
                      <p className="font-semibold text-sm">{addr.addressTitle || addr.title}</p>
                      {selectedBillingId === addr.id && <CheckCircle2 className="h-4 w-4 text-violet-400" />}
                    </div>
                    <p className="text-sm text-foreground">{addr.addressLine || addr.street}</p>
                    <p className="text-xs text-muted-foreground mt-1 line-clamp-2">{addr.district || addr.state}, {addr.city}</p>
                    <p className="text-xs text-muted-foreground">{addr.country}</p>
                  </button>
                ))}
              </div>
            )}
          </section>

          {/* Payment (Mock) */}
          <section className="space-y-4">
            <h2 className="text-xl font-semibold flex items-center gap-2">
              <span className="flex h-6 w-6 items-center justify-center rounded-full bg-violet-600 text-xs text-white">3</span>
              Ödeme Bilgileri
            </h2>
            <div className="rounded-xl border border-border/50 bg-card p-6">
              <div className="flex items-center gap-3 mb-4 text-muted-foreground">
                <CreditCard className="h-5 w-5" />
                <p className="text-sm">FAZ 3: Gerçek ödeme entegrasyonu (Iyzico / Stripe) henüz eklenmedi. "Siparişi Onayla" butonu doğrudan siparişi oluşturacaktır.</p>
              </div>
            </div>
          </section>

          {createOrder.error && <ErrorMessage error={createOrder.error} />}
        </div>

        {/* Right Column: Order Summary */}
        <div className="lg:col-span-1">
          <div className="sticky top-24 rounded-xl border border-border/50 bg-card p-6 shadow-sm">
            <h3 className="font-bold text-lg mb-4">Sipariş Özeti</h3>
            
            <div className="space-y-4 mb-6 max-h-[300px] overflow-y-auto pr-2">
              {items.map((item) => (
                <div key={`${item.productId}-${item.variant}`} className="flex gap-3">
                  <div className="h-12 w-12 rounded-md bg-muted overflow-hidden shrink-0">
                    {item.imageUrl ? (
                      <img src={item.imageUrl} alt="" className="h-full w-full object-cover" />
                    ) : (
                      <MapPin className="h-4 w-4 m-4 text-muted-foreground/30" />
                    )}
                  </div>
                  <div className="min-w-0 flex-1">
                    <p className="text-sm font-medium truncate">{item.name}</p>
                    <p className="text-xs text-muted-foreground">
                      {item.quantity} adet {item.variant && `| ${item.variant}`}
                    </p>
                    <p className="text-sm font-semibold mt-0.5">
                      {(item.price * item.quantity).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div className="space-y-3 pt-4 border-t border-border/50 text-sm">
              <div className="flex justify-between">
                <span className="text-muted-foreground">Ara Toplam</span>
                <span>{totalPrice().toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted-foreground">Kargo</span>
                <span className="text-emerald-400 font-medium">Ücretsiz</span>
              </div>
              <div className="flex justify-between pt-3 border-t border-border/50 font-bold text-lg">
                <span>Toplam</span>
                <span className="text-violet-400">
                  {totalPrice().toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                </span>
              </div>
            </div>

            <Button
              className="w-full mt-6 bg-gradient-to-r from-violet-600 to-indigo-600 hover:from-violet-700 hover:to-indigo-700 shadow-lg"
              size="lg"
              onClick={handleCheckout}
              disabled={createOrder.isPending || !selectedShippingId || (!sameAsShipping && !selectedBillingId) || items.length === 0}
            >
              {createOrder.isPending ? 'Sipariş Oluşturuluyor…' : 'Siparişi Onayla ve Bitir'}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
