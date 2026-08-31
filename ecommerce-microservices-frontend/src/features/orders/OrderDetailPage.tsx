// @ts-nocheck
import { useParams, Link, useLocation } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, Clock, MapPin, Package, Truck, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { DeleteConfirmModal } from '@/components/ui/delete-confirm-modal';
import { useMyOrder, useCancelMyOrder } from './useOrderQueries';
import { useState, useEffect } from 'react';
import { fetchAddressById, fetchProfileByUserId } from '@/features/crm/crmService';
import { toast } from 'sonner';
import type { OrderStatus } from './types';
import { formatOrderNumber } from '@/utils/formatters';

const STATUS_CONFIG: Record<OrderStatus, { label: string; icon: React.ReactNode; color: string; step: number }> = {
  CREATED: { label: 'Oluşturuldu', icon: <Clock className="w-6 h-6" />, color: 'text-gray-500', step: 0 },
  CREATED: { label: 'Oluşturuldu', icon: <Clock className="w-6 h-6" />, color: 'text-gray-500', step: 0 },
  PENDING: { label: 'Bekliyor', icon: <Clock className="h-5 w-5" />, color: 'text-amber-500 bg-amber-500/10 border-amber-500/20', step: 1 },
  PREPARING: { label: 'Hazırlanıyor', icon: <Package className="h-5 w-5" />, color: 'text-blue-500 bg-blue-500/10 border-blue-500/20', step: 2 },
  SHIPPED: { label: 'Kargoya Verildi', icon: <Truck className="h-5 w-5" />, color: 'text-violet-500 bg-violet-500/10 border-violet-500/20', step: 3 },
  DELIVERED: { label: 'Teslim Edildi', icon: <CheckCircle2 className="h-5 w-5" />, color: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20', step: 4 },
  CANCELLED: { label: 'İptal Edildi', icon: <XCircle className="h-5 w-5" />, color: 'text-destructive bg-destructive/10 border-destructive/20', step: -1 },
};

export function OrderDetailPage() {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const justCreated = location.state?.justCreated;

  const { data: order, isLoading, isError, error, refetch } = useMyOrder(id!);
  const cancelOrder = useCancelMyOrder();
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [addressDetails, setAddressDetails] = useState<any>(null);
  const [customerProfile, setCustomerProfile] = useState<any>(null);

  const handleCancelOrder = () => {
    if (!window.confirm("Siparişi iptal etmek istediğinize emin misiniz?")) {
      return;
    }

    if (order?.id) {
      cancelOrder.mutate(order.id, {
        onSuccess: () => {
          toast.success('Sipariş başarıyla iptal edildi');
          setTimeout(() => {
            window.location.reload();
          }, 500);
        },
        onError: () => {
          toast.error('Sipariş iptal edilirken bir hata oluştu');
        }
      });
    }
  };

  useEffect(() => {
    const addressId = (order as any)?.addressId || (order as any)?.shippingAddressId;
    if (addressId) {
      fetchAddressById(addressId)
        .then((data) => setAddressDetails(data))
        .catch((err) => console.error("Adres detayları çekilemedi:", err));
    }

    if ((order as any)?.keycloakUserId) {
      fetchProfileByUserId((order as any).keycloakUserId)
        .then((res) => {
          setCustomerProfile(res);
        })
        .catch((err) => {
          console.error("Failed to fetch customer profile for order", err);
        });
    }
  }, [order]);

  console.log("Gelen Sipariş Detayı:", order);

  if (isLoading || !order) {
    return (
      <div className="container mx-auto px-4 py-8 max-w-4xl space-y-6">
        <Skeleton className="h-10 w-1/3" />
        <Skeleton className="h-40 w-full" />
        <Skeleton className="h-64 w-full" />
      </div>
    );
  }

  if (isError) return <ErrorMessage error={error} onRetry={refetch} />;

  const statusConfig = STATUS_CONFIG[order?.status as OrderStatus] || STATUS_CONFIG.PENDING;
  
  const statusStr = (order?.status || '').toUpperCase();
  const isCancellable = statusStr === 'PENDING' || statusStr === 'CREATED' || statusStr === 'BEKLİYOR';

  // DTO Mappings (Fallback)
  const items = (order as any)?.items || (order as any)?.orderItems || [];
  const total = (order as any)?.totalAmount || (order as any)?.totalPrice || (order as any)?.total || 0;
  const address = addressDetails || (order as any)?.shippingAddress || (order as any)?.deliveryAddress || (order as any)?.address || {};
  const billingAddress = (order as any)?.billingAddress || address;

  const customerFullName = customerProfile?.firstName && customerProfile?.lastName
    ? `${customerProfile.firstName} ${customerProfile.lastName}`
    : customerProfile?.firstName || address?.fullName || address?.recipientName || order?.customerName || order?.userEmail || 'Müşteri';

  const billingCustomerFullName = customerProfile?.firstName && customerProfile?.lastName
    ? `${customerProfile.firstName} ${customerProfile.lastName}`
    : customerProfile?.firstName || billingAddress?.fullName || billingAddress?.recipientName || order?.customerName || order?.userEmail || 'Müşteri';

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl space-y-6">
      {justCreated && (
        <div className="flex items-center gap-3 p-4 rounded-xl border border-emerald-500/30 bg-emerald-500/10">
          <CheckCircle2 className="h-6 w-6 text-emerald-500" />
          <div>
            <h3 className="font-bold text-emerald-500">Siparişiniz Başarıyla Alındı!</h3>
            <p className="text-sm text-emerald-500/80" title={order?.id || id}>
              Sipariş numaranız: {order?.orderCode || order?.orderNumber || formatOrderNumber(order?.id || id)}
            </p>
          </div>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link to="/orders"><ArrowLeft className="h-5 w-5" /></Link>
          </Button>
          <div>
            <h1 className="text-xl font-bold">Sipariş Detayı</h1>
            <p className="text-sm text-muted-foreground font-mono" title={order?.id || id}>
              {order?.orderCode || order?.orderNumber || formatOrderNumber(order?.id || id)}
            </p>
          </div>
        </div>
        
        <div className="flex items-center gap-3">
          {isCancellable && (
            <button onClick={handleCancelOrder} className="bg-red-600 text-white px-4 py-2 rounded text-sm hover:bg-red-700 transition-colors disabled:opacity-50" disabled={cancelOrder.isPending}>
              Siparişi İptal Et
            </button>
          )}
          <Badge variant="outline" className={`px-3 py-1.5 flex items-center gap-2 text-sm ${statusConfig.color}`}>
            {statusConfig.icon}
            {statusConfig.label}
          </Badge>
        </div>
      </div>

      {/* Status Timeline */}
      {order?.status !== 'CANCELLED' && (
        <div className="rounded-xl border border-border/50 bg-card p-6 overflow-hidden">
          <div className="relative flex justify-between">
            <div className="absolute top-1/2 left-0 right-0 h-1 bg-muted -translate-y-1/2 z-0">
              <div 
                className="h-full bg-violet-500 transition-all duration-500" 
                style={{ width: `${((statusConfig.step - 1) / 3) * 100}%` }} 
              />
            </div>
            
            {['PENDING', 'PREPARING', 'SHIPPED', 'DELIVERED'].map((s) => {
              const cfg = STATUS_CONFIG[s as OrderStatus];
              const isCompleted = cfg.step <= statusConfig.step;
              const isCurrent = cfg.step === statusConfig.step;
              return (
                <div key={s} className="relative z-10 flex flex-col items-center gap-2">
                  <div className={`flex h-10 w-10 items-center justify-center rounded-full border-2 transition-colors ${
                    isCompleted ? 'border-violet-500 bg-violet-600 text-white' : 'border-muted-foreground/30 bg-card text-muted-foreground/50'
                  } ${isCurrent ? 'ring-4 ring-violet-500/20' : ''}`}>
                    {cfg.icon}
                  </div>
                  <span className={`text-xs font-medium hidden sm:block ${isCompleted ? 'text-foreground' : 'text-muted-foreground/50'}`}>
                    {cfg.label}
                  </span>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Two columns: Items and Info */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        
        {/* Items */}
        <div className="lg:col-span-2 space-y-4">
          <h2 className="font-semibold text-lg">Ürünler</h2>
          <div className="rounded-xl border border-border/50 bg-card divide-y divide-border/50">
            {items?.map((item: any, idx: number) => (
              <div key={`${item?.productId}-${idx}`} className="flex gap-4 p-4">
                <div className="h-20 w-20 rounded-md bg-muted overflow-hidden shrink-0 border">
                  {item?.imageUrl ? (
                    <img src={item.imageUrl} alt={item?.productName || ''} className="h-full w-full object-cover" />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center">
                      <Package className="h-8 w-8 text-muted-foreground/30" />
                    </div>
                  )}
                </div>
                <div className="flex flex-col flex-1 min-w-0">
                  <Link to={`/products/${item?.productId}`} className="font-semibold hover:text-violet-400 truncate transition-colors">
                    {item?.productName}
                  </Link>
                  {item?.variant && <p className="text-xs text-muted-foreground mt-0.5">{item.variant}</p>}
                  {item?.productCode && <p className="text-xs text-muted-foreground mt-0.5 font-mono">{item.productCode}</p>}
                  <div className="mt-auto flex items-center justify-between">
                    <p className="text-sm">
                      {(item?.unitPrice || item?.price || 0).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })} 
                      <span className="text-muted-foreground"> x {item?.quantity || 1}</span>
                    </p>
                    <p className="font-semibold">
                      {(item?.totalPrice || ((item?.unitPrice || item?.price || 0) * (item?.quantity || 1))).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                    </p>
                  </div>
                </div>
              </div>
            ))}
            
            {/* Totals */}
            <div className="p-4 space-y-2 text-sm">
              <div className="flex justify-between text-muted-foreground">
                <span>Ara Toplam</span>
                <span>{total?.toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}</span>
              </div>
              <div className="flex justify-between text-muted-foreground">
                <span>Kargo</span>
                <span className="text-emerald-400">Ücretsiz</span>
              </div>
              <div className="flex justify-between pt-2 border-t font-bold text-lg">
                <span>Genel Toplam</span>
                <span className="text-violet-400">
                  {total?.toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Addresses & Info */}
        <div className="space-y-6">
          <div className="rounded-xl border border-border/50 bg-card p-5">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2 flex items-center gap-2">
              <MapPin className="h-4 w-4 text-violet-400" />
              Teslimat Adresi
            </h4>
            
            {addressDetails === null && ((order as any)?.addressId || (order as any)?.shippingAddressId) ? (
              <p className="text-xs text-muted-foreground mt-1">Adres bilgisi yükleniyor...</p>
            ) : address ? (
              <>
                <div className="text-sm font-medium text-foreground mb-3">
                  {customerFullName}
                </div>
                <div className="mt-2 space-y-1 text-sm text-muted-foreground">
                  {address.title && (
                    <span className="inline-block px-2 py-0.5 rounded bg-muted text-foreground font-semibold mb-2 text-xs">
                      {address.title}
                    </span>
                  )}
                  {address.phone && <p>{address.phone}</p>}
                  <p className="leading-relaxed">
                    {address.addressLine || address.street || address.fullAddress || '-'}
                  </p>
                  <p className="opacity-80">
                    {[address.district || address.state, address.city, address.country, address.zipCode || address.postalCode].filter(Boolean).join(' / ')}
                  </p>
                </div>
              </>
            ) : (
              <p className="text-xs text-destructive mt-1">Adres detayları alınamadı</p>
            )}
          </div>
          
          <div className="rounded-xl border border-border/50 bg-card p-5">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2 flex items-center gap-2">
              <MapPin className="h-4 w-4 text-indigo-400" />
              Fatura Adresi
            </h4>
            
            {addressDetails === null && ((order as any)?.addressId || (order as any)?.shippingAddressId) ? (
              <p className="text-xs text-muted-foreground mt-1">Adres bilgisi yükleniyor...</p>
            ) : billingAddress ? (
              <>
                <div className="text-sm font-medium text-foreground mb-3">
                  {billingCustomerFullName}
                </div>
                <div className="mt-2 space-y-1 text-sm text-muted-foreground">
                  {billingAddress.title && (
                    <span className="inline-block px-2 py-0.5 rounded bg-muted text-foreground font-semibold mb-2 text-xs">
                      {billingAddress.title}
                    </span>
                  )}
                  {billingAddress.phone && <p>{billingAddress.phone}</p>}
                  <p className="leading-relaxed">
                    {billingAddress.addressLine || billingAddress.street || billingAddress.fullAddress || '-'}
                  </p>
                  <p className="opacity-80">
                    {[billingAddress.district || billingAddress.state, billingAddress.city, billingAddress.country, billingAddress.zipCode || billingAddress.postalCode].filter(Boolean).join(' / ')}
                  </p>
                </div>
              </>
            ) : (
              <p className="text-xs text-destructive mt-1">Adres detayları alınamadı</p>
            )}
          </div>

          <div className="rounded-xl border border-border/50 bg-card p-5 text-sm">
            <p className="text-muted-foreground mb-1">Sipariş Tarihi</p>
            <p className="font-medium">{order?.createdAt ? new Date(order.createdAt).toLocaleString('tr-TR') : '-'}</p>
          </div>
        </div>
      </div>

      <DeleteConfirmModal
        open={cancelModalOpen}
        onOpenChange={setCancelModalOpen}
        title="Siparişi İptal Et"
        description="Bu siparişi iptal etmek istediğinize emin misiniz? Bu işlem geri alınamaz."
        onConfirm={() => {
          if (order?.id) {
            cancelOrder.mutate(order.id, { 
              onSuccess: () => {
                setCancelModalOpen(false);
                toast.success('Sipariş başarıyla iptal edildi');
              },
              onError: () => {
                setCancelModalOpen(false);
                toast.error('Sipariş iptal edilirken bir hata oluştu');
              }
            });
          }
        }}
        isLoading={cancelOrder.isPending}
      />
    </div>
  );
}
