import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Package, ChevronRight, Clock, CheckCircle2, Truck, XCircle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorMessage } from '@/components/ui/error-message';
import { Pagination } from '@/components/ui/pagination';
import { useMyOrders } from './useOrderQueries';
import type { OrderStatus } from './types';

const PAGE_SIZE = 10;

const STATUS_CONFIG: Record<OrderStatus, { label: string; icon: React.ReactNode; color: string }> = {
  PENDING: { label: 'Bekliyor', icon: <Clock className="h-4 w-4" />, color: 'text-amber-500 bg-amber-500/10 border-amber-500/20' },
  PREPARING: { label: 'Hazırlanıyor', icon: <Package className="h-4 w-4" />, color: 'text-blue-500 bg-blue-500/10 border-blue-500/20' },
  SHIPPED: { label: 'Kargoya Verildi', icon: <Truck className="h-4 w-4" />, color: 'text-violet-500 bg-violet-500/10 border-violet-500/20' },
  DELIVERED: { label: 'Teslim Edildi', icon: <CheckCircle2 className="h-4 w-4" />, color: 'text-emerald-500 bg-emerald-500/10 border-emerald-500/20' },
  CANCELLED: { label: 'İptal Edildi', icon: <XCircle className="h-4 w-4" />, color: 'text-destructive bg-destructive/10 border-destructive/20' },
};

export function CustomerOrdersPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading, isError, error, refetch } = useMyOrders({ page, size: PAGE_SIZE, sort: 'createdAt,desc' });

  const rawOrderList = Array.isArray(data) ? data : (data?.content || []);
  const orderList = [...rawOrderList].sort((a: any, b: any) => {
    const timeA = new Date(a?.createdAt || a?.orderDate || a?.createdDate || 0).getTime();
    const timeB = new Date(b?.createdAt || b?.orderDate || b?.createdDate || 0).getTime();
    return timeB - timeA;
  });

  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-2xl font-bold mb-6">Siparişlerim</h1>

      {isError ? (
        <ErrorMessage error={error} onRetry={refetch} />
      ) : isLoading ? (
        <div className="space-y-4">
          <p className="text-muted-foreground animate-pulse mb-2">Yükleniyor...</p>
          {Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-32 w-full rounded-xl" />)}
        </div>
      ) : orderList.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-4 py-16 rounded-xl border border-dashed border-border/50 text-center">
          <div className="rounded-full bg-muted p-6">
            <Package className="h-12 w-12 text-muted-foreground/40" />
          </div>
          <div>
            <p className="font-semibold text-lg">Henüz bir siparişiniz bulunmamaktadır.</p>
            <p className="text-muted-foreground">İlk siparişinizi vermek için ürünlerimizi inceleyebilirsiniz.</p>
          </div>
          <Button asChild className="mt-2"><Link to="/products">Alışverişe Başla</Link></Button>
        </div>
      ) : (
        <div className="space-y-4">
          {orderList.map((order: any) => {
            const statusConfig = STATUS_CONFIG[order?.status as OrderStatus] || STATUS_CONFIG.PENDING;
            const items = order?.items || order?.orderItems || [];
            
            return (
              <div key={order?.id || order?.orderNumber} className="rounded-xl border border-border/50 bg-card overflow-hidden transition-all hover:border-violet-500/30">
                {/* Header */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-4 bg-muted/20 border-b border-border/50">
                  <div className="grid grid-cols-2 sm:flex gap-x-8 gap-y-2 text-sm">
                    <div>
                      <p className="text-muted-foreground mb-0.5">Sipariş Tarihi</p>
                      <p className="font-medium">
                        {(order?.createdAt || order?.orderDate || order?.createdDate) 
                          ? new Date(order?.createdAt || order?.orderDate || order?.createdDate).toLocaleDateString('tr-TR') 
                          : '-'}
                      </p>
                    </div>
                    <div>
                      <p className="text-muted-foreground mb-0.5">Sipariş Özeti</p>
                      <p className="font-medium">{items?.length || 0} Ürün</p>
                    </div>
                    <div>
                      <p className="text-muted-foreground mb-0.5">Toplam Tutar</p>
                      <p className="font-bold text-violet-400">
                        {(order?.totalAmount || order?.totalPrice || 0).toLocaleString('tr-TR', { style: 'currency', currency: 'TRY' })}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center justify-between sm:justify-end gap-4 w-full sm:w-auto">
                    <Badge variant="outline" className={`px-2.5 py-1 flex items-center gap-1.5 ${statusConfig.color}`}>
                      {statusConfig.icon}
                      {statusConfig.label}
                    </Badge>
                    <Button variant="outline" size="sm" asChild className="shrink-0">
                      <Link to={`/orders/${order?.id || order?.orderNumber}`}>
                        Detaylar <ChevronRight className="h-4 w-4 ml-1" />
                      </Link>
                    </Button>
                  </div>
                </div>

                {/* Items preview */}
                <div className="p-4 flex gap-3 overflow-x-auto">
                  {items.map((item: any, idx: number) => (
                    <div key={item?.id || item?.productId || idx} className="flex items-center gap-3 bg-card border rounded-lg p-2 pr-4 shrink-0 shadow-sm">
                      <div className="relative h-12 w-12 rounded-md bg-muted overflow-hidden shrink-0 border border-border/50">
                        {item?.imageUrl ? (
                          <img src={item.imageUrl} alt={item?.productName || item?.title || "Ürün"} className="h-full w-full object-cover" />
                        ) : (
                          <div className="flex h-full w-full items-center justify-center">
                            <Package className="h-5 w-5 text-muted-foreground/30" />
                          </div>
                        )}
                      </div>
                      <div className="flex flex-col justify-center">
                        <span className="text-sm font-medium truncate max-w-[120px] sm:max-w-[200px]" title={item?.productName || item?.title || "Ürün"}>
                          {item?.productName || item?.title || "Ürün"}
                        </span>
                        <span className="text-xs text-muted-foreground mt-0.5">
                          {item?.quantity || 1} adet
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            );
          })}

          {!Array.isArray(data) && data?.totalPages > 1 && (
            <Pagination
              currentPage={data.number}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              pageSize={data.size}
              onPageChange={setPage}
            />
          )}
        </div>
      )}
    </div>
  );
}
